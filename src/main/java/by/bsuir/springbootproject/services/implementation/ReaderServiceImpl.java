package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.ContinueReadingInfo;
import by.bsuir.springbootproject.dto.ReaderData;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.ComicPageRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.ReaderService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReaderServiceImpl implements ReaderService {

    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityContextUtils securityContextUtils;
    private final ChapterRepository chapterRepository;

    @Override
    public ReaderData getReaderData(Integer translationId) {
        Translation translation = translationRepository.findReaderTranslationById(translationId)
                .orElseThrow(() -> new RuntimeException("Перевод не найден или не одобрен: " + translationId));

        List<ComicPage> pages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId);
        if (pages.isEmpty()) {
            throw new RuntimeException("Страницы перевода отсутствуют: " + translationId);
        }

        Integer comicId = translation.getChapter().getComic().getId();
        Integer chapterNumber = translation.getChapter().getChapterNumber();
        Integer languageId = translation.getLanguage().getId();

        Integer prevChapterNumber = chapterRepository.findPrevChapterNumber(comicId, chapterNumber).orElse(null);
        Integer nextChapterNumber = chapterRepository.findNextChapterNumber(comicId, chapterNumber).orElse(null);

        Translation prev = resolveAdjacentTranslation(comicId, prevChapterNumber, languageId);
        Translation next = resolveAdjacentTranslation(comicId, nextChapterNumber, languageId);

        return new ReaderData(
                translation.getChapter().getComic(),
                translation,
                pages,
                prev,
                next
        );
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
    public Integer getFirstAvailableTranslationId(Integer comicId) {
        return translationRepository.findFirstApprovedByComic(comicId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(Translation::getId)
                .orElse(null);
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
                .<Set<Integer>>map(user -> {
                    String placeholders = translationIds.stream()
                            .map(id -> "?")
                            .collect(Collectors.joining(","));

                    List<Object> params = new ArrayList<>();
                    params.add(user.getId());
                    params.addAll(translationIds);

                    return new HashSet<>(jdbcTemplate.query(
                            "select translation_id from read_progress where user_id = ? and translation_id in (" + placeholders + ")",
                            (rs, rowNum) -> rs.getInt("translation_id"),
                            params.toArray()
                    ));
                })
                .orElse(Set.of());
    }

    @Override
    @Transactional
    public void markChapterReadIfAuthenticated(Integer chapterId) {
        securityContextUtils.getUserFromContext().ifPresent(user -> jdbcTemplate.update("""
                insert into read_chapters(user_id, chapter_id)
                values (?, ?)
                on conflict do nothing
                """, user.getId(), chapterId));
    }

    @Override
    @Transactional
    public void markTranslationOpenedIfAuthenticated(Integer translationId) {
        securityContextUtils.getUserFromContext().ifPresent(user -> jdbcTemplate.update("""
                insert into read_progress(user_id, translation_id, current_page, updated_at)
                values (?, ?, 1, now())
                on conflict (user_id, translation_id) do nothing
                """, user.getId(), translationId));
    }

    @Override
    public Integer getSavedPageIfAuthenticated(Integer translationId) {
        return securityContextUtils.getUserFromContext()
                .map(user -> {
                    try {
                        Integer page = jdbcTemplate.queryForObject("""
                                select current_page
                                from read_progress
                                where user_id = ? and translation_id = ?
                                """, Integer.class, user.getId(), translationId);

                        return page != null && page > 0 ? page : 1;
                    } catch (EmptyResultDataAccessException e) {
                        return 1;
                    }
                })
                .orElse(1);
    }

    @Override
    @Transactional
    public void saveProgressIfAuthenticated(Integer translationId, Integer page) {
        if (page == null || page < 1) {
            return;
        }

        securityContextUtils.getUserFromContext().ifPresent(user -> jdbcTemplate.update("""
                insert into read_progress(user_id, translation_id, current_page, updated_at)
                values (?, ?, ?, now())
                on conflict (user_id, translation_id) do update
                set current_page = excluded.current_page,
                    updated_at = now()
                """, user.getId(), translationId, page));
    }

    private Optional<ReadProgressSnapshot> findLastReadProgress(Integer userId, Integer comicId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    select rp.translation_id, rp.current_page
                    from read_progress rp
                    join translations t on t.translation_id = rp.translation_id
                    join chapters ch on ch.chapter_id = t.chapter_id
                    where rp.user_id = ?
                      and ch.comic_id = ?
                    order by rp.updated_at desc, rp.translation_id desc
                    limit 1
                    """, (rs, rowNum) -> new ReadProgressSnapshot(
                    rs.getInt("translation_id"),
                    rs.getInt("current_page")
            ), userId, comicId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private record ReadProgressSnapshot(Integer translationId, Integer currentPage) {
    }
}
