package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.ContinueReadingInfo;
import by.bsuir.springbootproject.dto.ReaderData;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.ReadProgress;
import by.bsuir.springbootproject.entities.ReadProgressId;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.ComicPageRepository;
import by.bsuir.springbootproject.repositories.ReadProgressRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.services.ReaderService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReaderServiceImpl implements ReaderService {

    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final SecurityContextUtils securityContextUtils;
    private final ChapterRepository chapterRepository;
    private final ReadProgressRepository readProgressRepository;
    private final UserRepository userRepository;

    @Override
    public ReaderData getReaderData(Integer translationId, boolean allowUnapprovedPreview) {
        LoadedTranslation loadedTranslation = loadTranslationForReader(translationId, allowUnapprovedPreview);
        Translation translation = loadedTranslation.translation();

        List<ComicPage> pages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId);
        if (pages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Страницы перевода отсутствуют: " + translationId);
        }

        Translation prev = null;
        Translation next = null;

        if (!loadedTranslation.previewMode()) {
            Integer comicId = translation.getChapter().getComic().getId();
            Integer chapterNumber = translation.getChapter().getChapterNumber();
            Integer languageId = translation.getLanguage().getId();

            Integer prevChapterNumber = chapterRepository.findPrevChapterNumber(comicId, chapterNumber).orElse(null);
            Integer nextChapterNumber = chapterRepository.findNextChapterNumber(comicId, chapterNumber).orElse(null);

            prev = resolveAdjacentTranslation(comicId, prevChapterNumber, languageId);
            next = resolveAdjacentTranslation(comicId, nextChapterNumber, languageId);
        }

        return new ReaderData(
                translation.getChapter().getComic(),
                translation,
                pages,
                prev,
                next
        );
    }

    private LoadedTranslation loadTranslationForReader(Integer translationId, boolean allowUnapprovedPreview) {
        if (allowUnapprovedPreview) {
            Translation previewTranslation = translationRepository.findReaderPreviewById(translationId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Перевод не найден: " + translationId
                    ));

            validatePreviewAccess(previewTranslation);
            return new LoadedTranslation(previewTranslation, true);
        }

        Optional<Translation> approvedTranslation = translationRepository.findReaderTranslationById(translationId);
        if (approvedTranslation.isPresent()) {
            return new LoadedTranslation(approvedTranslation.get(), false);
        }

        Translation previewTranslation = translationRepository.findReaderPreviewById(translationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Перевод не найден: " + translationId
                ));

        validatePreviewAccess(previewTranslation);
        return new LoadedTranslation(previewTranslation, true);
    }

    private void validatePreviewAccess(Translation translation) {
        boolean allowed = securityContextUtils.getUserFromContext()
                .map(user -> isAdmin(user) || isOwner(user, translation))
                .orElse(false);

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "У вас нет доступа к этому переводу");
        }
    }

    private boolean isOwner(User user, Translation translation) {
        return user != null
                && user.getId() != null
                && translation.getUser() != null
                && translation.getUser().getId() != null
                && user.getId().equals(translation.getUser().getId());
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getName() != null
                && Set.of("ADMIN", "ROLE_ADMIN").contains(user.getRole().getName().trim().toUpperCase(Locale.ROOT));
    }

    private Translation resolveAdjacentTranslation(Integer comicId, Integer targetChapterNumber, Integer languageId) {
        if (targetChapterNumber == null) {
            return null;
        }

        return translationRepository.findApprovedSameLanguageByComicAndChapterNumber(
                        comicId,
                        targetChapterNumber,
                        languageId
                ).stream()
                .findFirst()
                .orElseGet(() -> translationRepository.findApprovedAnyLanguageByComicAndChapterNumber(
                                comicId,
                                targetChapterNumber
                        ).stream()
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public ContinueReadingInfo getContinueReadingInfoIfAuthenticated(Integer comicId) {
        return securityContextUtils.getUserFromContext()
                .flatMap(user -> findLastReadProgress(user.getId(), comicId))
                .flatMap(progress -> translationRepository.findReaderTranslationById(progress.translationId())
                        .map(translation -> new ContinueReadingInfo(
                                translation.getId(),
                                translation.getChapter().getChapterNumber(),
                                translation.getLanguage().getName(),
                                progress.currentPage()
                        )))
                .orElse(null);
    }

    @Override
    public Set<Integer> getReadTranslationIdsIfAuthenticated(List<Integer> translationIds) {
        if (translationIds == null || translationIds.isEmpty()) {
            return Set.of();
        }

        return securityContextUtils.getUserFromContext()
                .map(user -> readProgressRepository.findByUser_IdAndTranslation_IdIn(user.getId(), translationIds).stream()
                        .map(readProgress -> readProgress.getTranslation().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    @Override
    @Transactional
    public void markTranslationOpenedIfAuthenticated(Integer translationId) {
        securityContextUtils.getUserFromContext().ifPresent(user -> {
            ReadProgressId id = new ReadProgressId(user.getId(), translationId);
            if (readProgressRepository.existsById(id)) {
                return;
            }

            ReadProgress readProgress = new ReadProgress();
            readProgress.setId(id);
            readProgress.setUser(userRepository.getReferenceById(user.getId()));
            readProgress.setTranslation(translationRepository.getReferenceById(translationId));
            readProgress.setCurrentPage(1);
            readProgress.setUpdatedAt(LocalDateTime.now());

            readProgressRepository.save(readProgress);
        });
    }

    @Override
    public Integer getSavedPageIfAuthenticated(Integer translationId) {
        return securityContextUtils.getUserFromContext()
                .flatMap(user -> readProgressRepository.findByUser_IdAndTranslation_Id(user.getId(), translationId))
                .map(ReadProgress::getCurrentPage)
                .filter(page -> page > 0)
                .orElse(1);
    }

    @Override
    @Transactional
    public void saveProgressIfAuthenticated(Integer translationId, Integer page) {
        if (page == null || page < 1) {
            return;
        }

        securityContextUtils.getUserFromContext().ifPresent(user -> {
            ReadProgress readProgress = readProgressRepository.findByUser_IdAndTranslation_Id(user.getId(), translationId)
                    .orElseGet(() -> {
                        ReadProgress newProgress = new ReadProgress();
                        newProgress.setId(new ReadProgressId(user.getId(), translationId));
                        newProgress.setUser(userRepository.getReferenceById(user.getId()));
                        newProgress.setTranslation(translationRepository.getReferenceById(translationId));
                        return newProgress;
                    });

            readProgress.setCurrentPage(page);
            readProgress.setUpdatedAt(LocalDateTime.now());

            readProgressRepository.save(readProgress);
        });
    }

    @Override
    public List<String> getApprovedLanguagesByChapterId(Integer chapterId) {
        return translationRepository.findApprovedLangsByChapterIds(List.of(chapterId)).stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == chapterId)
                .map(row -> (String) row[1])
                .distinct()
                .toList();
    }

    private Optional<ReadProgressSnapshot> findLastReadProgress(Integer userId, Integer comicId) {
        return readProgressRepository.findLatestByUserIdAndComicId(userId, comicId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(progress -> new ReadProgressSnapshot(
                        progress.getTranslation().getId(),
                        progress.getCurrentPage()
                ));
    }

    private record ReadProgressSnapshot(Integer translationId, Integer currentPage) {
    }

    private record LoadedTranslation(Translation translation, boolean previewMode) {
    }
}
