package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Language;
import by.bsuir.springbootproject.entities.ReviewStatus;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.TranslationType;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.*;
import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.services.TranslationSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TranslationSubmissionServiceImpl implements TranslationSubmissionService {

    private static final String VIEW_FORM = "translation/submission-form";
    private static final String VIEW_PREVIEW = "translation/submission-preview";
    private static final String VIEW_REVIEW_LIST = "admin/translation-review-list";

    private static final String STATUS_PENDING = "На проверке";
    private static final String STATUS_APPROVED = "Одобрено";
    private static final String STATUS_REJECTED = "Отклонено";

    private static final String TYPE_OFFICIAL = "Официальный";
    private static final String TYPE_AMATEUR = "Любительский";
    private static final String TYPE_AUTOMATIC = "Автоматический";

    private static final int USER_PENDING_LIMIT = 5;
    private static final int MAX_PAGE_COUNT = 200;
    private static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;
    private static final int MAX_SEARCH_QUERY_LENGTH = 255;
    private static final int MODERATION_PAGE_SIZE = 10;

    private static final Pattern PAGE_FILE_PATTERN = Pattern.compile("^(\\d{1,3})\\.(jpg|webp)$", Pattern.CASE_INSENSITIVE);

    private static final Path PAGES_STORAGE_DIR = Paths.get("src/main/webapp/assets/pages");

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final LanguageRepository languageRepository;
    private final ReviewStatusRepository reviewStatusRepository;
    private final TranslationTypeRepository translationTypeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Override
    public ModelAndView getCreatePage(Integer comicId, User user, TranslationSubmissionForm form, String errorMessage) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден."));

        boolean admin = isAdmin(user);

        if (!admin) {
            ensureUserCanSubmit(user);
        }

        List<Language> languages = languageRepository.findAllByOrderByNameAsc();
        if (languages.isEmpty()) {
            throw new IllegalStateException("В системе нет доступных языков перевода.");
        }

        TranslationSubmissionForm actualForm = form != null ? form : new TranslationSubmissionForm();
        actualForm.setComicId(comicId);

        if (actualForm.getLanguageId() == null) {
            actualForm.setLanguageId(languages.get(0).getId());
        }
        if (!StringUtils.hasText(actualForm.getReadingDirection())) {
            actualForm.setReadingDirection("LTR");
        }
        if (actualForm.getAutoTranslate() == null) {
            actualForm.setAutoTranslate(false);
        }

        List<Integer> chapterOptions = getAllowedChapterNumbers(comicId, actualForm.getLanguageId());
        if (actualForm.getChapterNumber() == null && !chapterOptions.isEmpty()) {
            actualForm.setChapterNumber(chapterOptions.get(chapterOptions.size() - 1));
        }

        ModelAndView mv = new ModelAndView(VIEW_FORM);
        mv.addObject("comic", comic);
        mv.addObject("form", actualForm);
        mv.addObject("languages", languages);
        mv.addObject("chapterOptions", chapterOptions);
        mv.addObject("isAdmin", admin);
        mv.addObject("pendingCount", admin ? 0 : translationRepository.countByUser_IdAndReviewStatus_Name(user.getId(), STATUS_PENDING));
        mv.addObject("pendingLimit", USER_PENDING_LIMIT);
        mv.addObject("errorMessage", errorMessage);
        return mv;
    }

    @Override
    public Map<String, Object> getChapterOptions(Integer comicId, Integer languageId) {
        List<Integer> values = getAllowedChapterNumbers(comicId, languageId);
        Integer defaultValue = values.isEmpty() ? null : values.get(values.size() - 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chapterNumbers", values);
        result.put("defaultChapterNumber", defaultValue);
        return result;
    }

    @Override
    @Transactional
    public Integer submit(Integer comicId, User user, TranslationSubmissionForm form, MultipartFile[] pageFiles) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден."));

        boolean admin = isAdmin(user);
        if (!admin) {
            ensureUserCanSubmit(user);
        }

        if (form == null) {
            throw new IllegalArgumentException("Форма добавления главы не передана.");
        }

        Integer languageId = form.getLanguageId();
        Integer chapterNumber = form.getChapterNumber();

        if (languageId == null) {
            throw new IllegalArgumentException("Выберите язык перевода.");
        }
        if (chapterNumber == null) {
            throw new IllegalArgumentException("Выберите номер главы.");
        }

        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new IllegalArgumentException("Выбранный язык не найден."));

        List<Integer> allowedChapterNumbers = getAllowedChapterNumbers(comicId, languageId);
        if (!allowedChapterNumbers.contains(chapterNumber)) {
            throw new IllegalArgumentException("Номер главы недоступен для выбранного языка.");
        }

        String title = normalizeTitle(form.getTitle());
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Введите название перевода.");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Название перевода не должно превышать 255 символов.");
        }

        if (Boolean.TRUE.equals(form.getAutoTranslate())) {
            if (!admin) {
                throw new IllegalArgumentException("Обычные пользователи не могут запрашивать автоматический перевод.");
            }
            throw new IllegalStateException("Автоматический перевод пока не подключён.");
        }

        List<PageFileCandidate> sortedFiles = prepareFiles(pageFiles);

        Optional<Chapter> existingChapter = chapterRepository.findByComic_IdAndChapterNumber(comicId, chapterNumber);
        boolean newChapterCreated = existingChapter.isEmpty();

        Chapter chapter = existingChapter.orElseGet(() ->
                chapterRepository.save(Chapter.builder()
                        .comic(comic)
                        .chapterNumber(chapterNumber)
                        .build())
        );

        long approvedBefore = newChapterCreated ? 0L : translationRepository.countApprovedByChapterId(chapter.getId());

        ReviewStatus reviewStatus = reviewStatusRepository.findByName(admin ? STATUS_APPROVED : STATUS_PENDING)
                .orElseThrow(() -> new IllegalStateException("Статус проверки перевода не найден."));

        TranslationType translationType = translationTypeRepository.findByName(admin ? TYPE_OFFICIAL : TYPE_AMATEUR)
                .orElseThrow(() -> new IllegalStateException("Тип перевода не найден."));

        Translation translation = translationRepository.saveAndFlush(
                Translation.builder()
                        .chapter(chapter)
                        .language(language)
                        .translationType(translationType)
                        .user(user)
                        .title(title)
                        .reviewStatus(reviewStatus)
                        .createdAt(java.time.LocalDateTime.now())
                        .build()
        );

        List<String> savedFiles = new ArrayList<>();
        try {
            Files.createDirectories(PAGES_STORAGE_DIR);

            List<ComicPage> pages = new ArrayList<>();
            for (PageFileCandidate candidate : sortedFiles) {
                String savedFileName = buildStoredFileName(translation.getId(), candidate.pageNumber(), candidate.file().getOriginalFilename());
                Path targetPath = PAGES_STORAGE_DIR.resolve(savedFileName).normalize();

                candidate.file().transferTo(targetPath);
                savedFiles.add(savedFileName);

                pages.add(ComicPage.builder()
                        .translation(translation)
                        .pageNumber(candidate.pageNumber())
                        .imagePath(savedFileName)
                        .build());
            }

            comicPageRepository.saveAll(pages);
        } catch (IOException ex) {
            deleteStoredFiles(savedFiles);
            throw new IllegalStateException("Не удалось сохранить изображения страниц.");
        } catch (RuntimeException ex) {
            deleteStoredFiles(savedFiles);
            throw ex;
        }

        if (newChapterCreated) {
            comic.setChaptersCount((int) chapterRepository.countByComic_Id(comicId));
            comicRepository.save(comic);
        }

        if (!admin && !Boolean.TRUE.equals(user.getCanPropose())) {
            user.setCanPropose(true);
            userRepository.save(user);
        }

        if (admin) {
            if (approvedBefore == 0L) {
                notificationService.notifyNewChapterForSubscribers(chapter);
            }
            notificationService.notifyNewTranslationForSubscribers(translation);
        }

        return translation.getId();
    }

    @Override
    public ModelAndView getPreviewPage(Integer translationId, User viewer, String successMessage, String errorMessage) {
        Translation translation = translationRepository.findSubmissionPreviewById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        boolean admin = isAdmin(viewer);
        boolean owner = translation.getUser() != null
                && viewer != null
                && translation.getUser().getId().equals(viewer.getId());

        if (!admin && !owner) {
            throw new IllegalStateException("Недостаточно прав для просмотра этой главы.");
        }

        List<ComicPage> pages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId);

        ModelAndView mv = new ModelAndView(VIEW_PREVIEW);
        mv.addObject("translation", translation);
        mv.addObject("pages", pages);
        mv.addObject("comic", translation.getChapter().getComic());
        mv.addObject("isAdmin", admin);
        mv.addObject("isOwner", owner);
        mv.addObject("canModerate", admin && STATUS_PENDING.equals(translation.getReviewStatus().getName()));
        mv.addObject("revokeRightsChecked",
                translation.getUser() != null && Boolean.FALSE.equals(translation.getUser().getCanPropose()));
        mv.addObject("successMessage", successMessage);
        mv.addObject("errorMessage", errorMessage);
        return mv;
    }


    @Override
    public ModelAndView getModerationPage(String q, String sortDirection, int page) {
        String normalizedQ = normalizeSearchQuery(q);
        String normalizedSortDirection = normalizeSortDirection(sortDirection);

        Sort sort = "asc".equals(normalizedSortDirection)
                ? Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
                : Sort.by("createdAt").descending().and(Sort.by("id").descending());

        Page<Translation> pendingPage = translationRepository.findModerationPageByStatusNameAndQuery(
                STATUS_PENDING,
                normalizedQ,
                PageRequest.of(Math.max(page, 0), MODERATION_PAGE_SIZE, sort)
        );

        Map<Integer, Long> pageCounts = pendingPage.getContent()
                .stream()
                .collect(Collectors.toMap(
                        Translation::getId,
                        translation -> comicPageRepository.countByTranslation_Id(translation.getId()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        ModelAndView mv = new ModelAndView(VIEW_REVIEW_LIST);
        fillModerationModel(mv, pendingPage, pageCounts, normalizedQ, normalizedSortDirection);
        return mv;
    }
    private void fillModerationModel(
            ModelAndView mv,
            Page<Translation> pendingPage,
            Map<Integer, Long> pageCounts,
            String q,
            String sortDirection
    ) {
        int totalPages = pendingPage.getTotalPages();
        int currentPage = totalPages == 0 ? 1 : pendingPage.getNumber() + 1;

        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(Math.max(totalPages, 1), currentPage + 2);

        if (endPage - beginPage < 4) {
            beginPage = Math.max(1, endPage - 4);
            endPage = Math.min(Math.max(totalPages, 1), beginPage + 4);
        }

        mv.addObject("translations", pendingPage.getContent());
        mv.addObject("pendingPage", pendingPage);
        mv.addObject("pageCounts", pageCounts);
        mv.addObject("q", q == null ? "" : q);
        mv.addObject("sortDirection", "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc");
        mv.addObject("currentPage", currentPage);
        mv.addObject("currentPageZeroBased", Math.max(pendingPage.getNumber(), 0));
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
    }

    private String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        return normalized.length() > MAX_SEARCH_QUERY_LENGTH
                ? normalized.substring(0, MAX_SEARCH_QUERY_LENGTH)
                : normalized;
    }

    private String normalizeSortDirection(String value) {
        return "asc".equalsIgnoreCase(value) ? "asc" : "desc";
    }

    @Override
    @Transactional
    public void approve(Integer translationId, User admin) {
        requireAdmin(admin);

        Translation translation = translationRepository.findSubmissionPreviewById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        if (!STATUS_PENDING.equals(translation.getReviewStatus().getName())) {
            throw new IllegalStateException("Этот перевод уже был рассмотрен.");
        }

        ReviewStatus approvedStatus = reviewStatusRepository.findByName(STATUS_APPROVED)
                .orElseThrow(() -> new IllegalStateException("Статус «Одобрено» не найден."));

        long approvedBefore = translationRepository.countApprovedByChapterId(translation.getChapter().getId());

        translation.setReviewStatus(approvedStatus);
        translationRepository.save(translation);

        if (approvedBefore == 0L) {
            notificationService.notifyNewChapterForSubscribers(translation.getChapter());
        }
        notificationService.notifyNewTranslationForSubscribers(translation);

        if (translation.getUser() != null) {
            notificationService.notifyChapterApproved(translation.getUser().getId(), translation);
        }
    }

    @Override
    @Transactional
    public void reject(Integer translationId, User admin, String reason, boolean revokeRights) {
        requireAdmin(admin);

        Translation translation = translationRepository.findSubmissionPreviewById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        if (!STATUS_PENDING.equals(translation.getReviewStatus().getName())) {
            throw new IllegalStateException("Этот перевод уже был рассмотрен.");
        }

        String normalizedReason = normalizeRejectReason(reason);

        if (translation.getUser() != null) {
            notificationService.notifyChapterRejected(
                    translation.getUser().getId(),
                    translation,
                    normalizedReason
            );

            if (revokeRights && Boolean.TRUE.equals(translation.getUser().getCanPropose())) {
                User uploader = translation.getUser();
                uploader.setCanPropose(false);
                userRepository.save(uploader);

                notificationService.notifyUploadRightsRevoked(
                        uploader.getId(),
                        "Вам ограничили возможность добавлять главы."
                );
            }
        }

        deleteRejectedTranslation(translation);
    }

    private void ensureUserCanSubmit(User user) {
        if (user == null) {
            throw new IllegalStateException("Авторизуйтесь, чтобы добавить главу.");
        }

        long totalUserTranslations = translationRepository.countByUser_Id(user.getId());
        if (Boolean.FALSE.equals(user.getCanPropose()) && totalUserTranslations > 0) {
            throw new IllegalStateException("У вас нет прав на добавление глав.");
        }

        long pendingCount = translationRepository.countByUser_IdAndReviewStatus_Name(user.getId(), STATUS_PENDING);
        if (pendingCount >= USER_PENDING_LIMIT) {
            throw new IllegalStateException(
                    "У вас уже " + USER_PENDING_LIMIT + " переводов на проверке. Дождитесь рассмотрения хотя бы одного из них."
            );
        }
    }

    private void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new IllegalStateException("Недостаточно прав.");
        }
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }

    private List<Integer> getAllowedChapterNumbers(Integer comicId, Integer languageId) {
        if (languageId == null) {
            return List.of();
        }

        List<Integer> approvedChapterNumbers = new ArrayList<>(new LinkedHashSet<>(
                translationRepository.findDistinctApprovedChapterNumbersByComicAndLanguage(comicId, languageId)
        ));
        approvedChapterNumbers.sort(Integer::compareTo);

        int maxAllowedChapterNumber = approvedChapterNumbers.isEmpty()
                ? 1
                : approvedChapterNumbers.get(approvedChapterNumbers.size() - 1) + 1;

        List<Integer> result = new ArrayList<>(maxAllowedChapterNumber);
        for (int i = 1; i <= maxAllowedChapterNumber; i++) {
            result.add(i);
        }

        return result;
    }

    private void deleteRejectedTranslation(Translation translation) {
        Integer translationId = translation.getId();
        Integer chapterId = translation.getChapter() != null ? translation.getChapter().getId() : null;
        Integer comicId = translation.getChapter() != null && translation.getChapter().getComic() != null
                ? translation.getChapter().getComic().getId()
                : null;

        List<String> fileNames = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId)
                .stream()
                .map(ComicPage::getImagePath)
                .filter(StringUtils::hasText)
                .toList();

        notificationRepository.detachDeletedTranslation(translationId);
        comicPageRepository.deleteAllByTranslationId(translationId);
        translationRepository.deleteHardById(translationId);

        deleteStoredFiles(fileNames);
        cleanupEmptyChapter(chapterId, comicId);
    }

    private void cleanupEmptyChapter(Integer chapterId, Integer comicId) {
        if (chapterId == null || translationRepository.countByChapter_Id(chapterId) > 0) {
            return;
        }

        chapterRepository.deleteById(chapterId);

        if (comicId != null) {
            comicRepository.findById(comicId).ifPresent(comic -> {
                comic.setChaptersCount((int) chapterRepository.countByComic_Id(comicId));
                comicRepository.save(comic);
            });
        }
    }

    private String normalizeTitle(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeRejectReason(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            return "Перевод не прошёл проверку.";
        }
        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    private List<PageFileCandidate> prepareFiles(MultipartFile[] pageFiles) {
        if (pageFiles == null) {
            throw new IllegalArgumentException("Загрузите страницы перевода.");
        }

        List<MultipartFile> actualFiles = List.of(pageFiles).stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (actualFiles.isEmpty()) {
            throw new IllegalArgumentException("Загрузите страницы перевода.");
        }

        if (actualFiles.size() > MAX_PAGE_COUNT) {
            throw new IllegalArgumentException("Максимум можно загрузить " + MAX_PAGE_COUNT + " страниц.");
        }

        List<PageFileCandidate> candidates = new ArrayList<>();

        for (MultipartFile file : actualFiles) {
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("Каждое изображение должно быть не больше 1 МБ.");
            }

            String originalName = StringUtils.getFilename(file.getOriginalFilename());
            if (!StringUtils.hasText(originalName)) {
                throw new IllegalArgumentException("У всех файлов должны быть корректные имена.");
            }

            Matcher matcher = PAGE_FILE_PATTERN.matcher(originalName);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Можно загружать только файлы JPG и WEBP с именами вида 001.jpg, 002.jpg, 003.jpg или 001.webp, 002.webp, 003.webp.");
            }

            String contentType = file.getContentType();
            String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
            if (!"image/jpeg".equals(normalizedContentType) && !"image/webp".equals(normalizedContentType)) {
                throw new IllegalArgumentException("Можно загружать только файлы JPG и WEBP.");
            }

            int pageNumber = Integer.parseInt(matcher.group(1));
            candidates.add(new PageFileCandidate(pageNumber, file));
        }

        candidates.sort(Comparator.comparingInt(PageFileCandidate::pageNumber));

        for (int i = 0; i < candidates.size(); i++) {
            int expected = i + 1;
            if (candidates.get(i).pageNumber() != expected) {
                throw new IllegalArgumentException("Файлы страниц должны идти подряд: 001.jpg, 002.jpg, 003.jpg и так далее.");
            }
        }

        return candidates;
    }

    private String buildStoredFileName(Integer translationId, int pageNumber, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExtension = StringUtils.hasText(extension) ? extension.toLowerCase() : "jpg";

        return "tr_%d_p%03d_%s.%s".formatted(
                translationId,
                pageNumber,
                UUID.randomUUID().toString().replace("-", ""),
                normalizedExtension
        );
    }

    private void deleteStoredFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                Files.deleteIfExists(PAGES_STORAGE_DIR.resolve(fileName));
            } catch (IOException ignored) {
            }
        }
    }

    private record PageFileCandidate(int pageNumber, MultipartFile file) {
    }
}
