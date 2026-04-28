package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.AdminTranslationEditForm;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.ComicPageRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComplaintRepository;
import by.bsuir.springbootproject.repositories.NotificationRepository;
import by.bsuir.springbootproject.repositories.ReadProgressRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.AdminTranslationManageService;
import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.services.UploadStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTranslationManageServiceImpl implements AdminTranslationManageService {

    private static final String VIEW_EDIT = "admin/translation-edit";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String SCOPE_TRANSLATION = "TRANSLATION";
    private static final int MAX_PAGE_COUNT = 200;
    private static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int TEMP_PAGE_NUMBER_START = 100000;

    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final ChapterRepository chapterRepository;
    private final ComicRepository comicRepository;
    private final ReadProgressRepository readProgressRepository;
    private final NotificationRepository notificationRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final UploadStorageService uploadStorageService;
    private final ObjectMapper objectMapper;

    @Override
    public ModelAndView getEditPage(Integer translationId,
                                    User admin,
                                    AdminTranslationEditForm form,
                                    String errorMessage) {
        requireAdmin(admin);

        Translation translation = translationRepository.findAdminManageById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        List<ComicPage> pages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId);
        AdminTranslationEditForm actualForm = form != null
                ? form
                : new AdminTranslationEditForm(translation.getTitle());

        ModelAndView mv = new ModelAndView(VIEW_EDIT);
        mv.addObject("translation", translation);
        mv.addObject("comic", translation.getChapter().getComic());
        mv.addObject("pages", pages);
        mv.addObject("form", actualForm);
        mv.addObject("errorMessage", errorMessage);
        return mv;
    }

    @Override
    public Integer updateTranslation(Integer translationId,
                                     User admin,
                                     AdminTranslationEditForm form,
                                     String pagesPayload,
                                     MultipartHttpServletRequest multipartRequest) {
        requireAdmin(admin);

        Translation translation = translationRepository.findAdminManageById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        String normalizedTitle = normalizeTitle(form != null ? form.getTitle() : null);
        translation.setTitle(normalizedTitle);
        translationRepository.save(translation);

        applyPageChanges(translation, pagesPayload, multipartRequest);

        if (translation.getUser() != null) {
            String comicTitle = translation.getChapter().getComic().getTitle();
            Integer chapterNumber = translation.getChapter().getChapterNumber();
            String languageName = translation.getLanguage().getName();

            notificationService.notifyAdminMessage(
                    translation.getUser().getId(),
                    "Администрация отредактировала ваш перевод в тайтле «" + comicTitle + "», глава " + chapterNumber + " (" + languageName + ")."
            );
        }

        return translation.getId();
    }

    @Override
    public Integer deleteTranslation(Integer translationId, User admin) {
        requireAdmin(admin);

        Translation translation = translationRepository.findAdminManageById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Перевод не найден."));

        Integer comicId = translation.getChapter().getComic().getId();
        Integer chapterId = translation.getChapter().getId();
        Integer chapterNumber = translation.getChapter().getChapterNumber();
        String comicTitle = translation.getChapter().getComic().getTitle();
        String languageName = translation.getLanguage().getName();

        List<String> pageFileNames = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId).stream()
                .map(ComicPage::getImagePath)
                .filter(StringUtils::hasText)
                .toList();

        if (translation.getUser() != null) {
            notificationService.notifyUserChapterDeleted(
                    translation.getUser().getId(),
                    comicId,
                    comicTitle,
                    chapterNumber,
                    languageName,
                    "Администрация удалила ваш перевод."
            );
        }

        complaintRepository.deleteByTargetIdAndScope(translationId, SCOPE_TRANSLATION);
        notificationRepository.detachDeletedTranslation(translationId);
        readProgressRepository.deleteByTranslationId(translationId);
        comicPageRepository.deleteAllByTranslationId(translationId);
        translationRepository.deleteHardById(translationId);

        deleteStoredFiles(pageFileNames);
        cleanupEmptyChapter(chapterId, comicId);

        return comicId;
    }

    private void applyPageChanges(Translation translation,
                                  String pagesPayload,
                                  MultipartHttpServletRequest multipartRequest) {
        List<PageInstruction> instructions = parseInstructions(pagesPayload);
        if (instructions.isEmpty()) {
            throw new IllegalArgumentException("В переводе должна остаться хотя бы одна страница.");
        }

        if (instructions.size() > MAX_PAGE_COUNT) {
            throw new IllegalArgumentException("В переводе не может быть больше 200 страниц.");
        }

        List<ComicPage> existingPages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translation.getId());
        Map<Integer, ComicPage> existingById = new HashMap<>();
        for (ComicPage page : existingPages) {
            existingById.put(page.getId(), page);
        }

        Set<Integer> usedExistingIds = new HashSet<>();
        List<ComicPage> keptExistingPages = new ArrayList<>();

        for (PageInstruction instruction : instructions) {
            if (instruction.pageId() == null) {
                continue;
            }

            ComicPage page = existingById.get(instruction.pageId());
            if (page == null) {
                throw new IllegalArgumentException("Обнаружена несуществующая страница перевода.");
            }

            if (!usedExistingIds.add(instruction.pageId())) {
                throw new IllegalArgumentException("Одна и та же страница не может использоваться дважды.");
            }

            keptExistingPages.add(page);
        }

        List<ComicPage> removedPages = existingPages.stream()
                .filter(page -> !usedExistingIds.contains(page.getId()))
                .toList();

        List<String> oldFilesToDelete = new ArrayList<>(removedPages.stream()
                .map(ComicPage::getImagePath)
                .filter(StringUtils::hasText)
                .toList());

        if (!keptExistingPages.isEmpty()) {
            int tempNumber = TEMP_PAGE_NUMBER_START;
            for (ComicPage keptPage : keptExistingPages) {
                keptPage.setPageNumber(tempNumber++);
            }
            comicPageRepository.saveAll(keptExistingPages);
            comicPageRepository.flush();
        }

        if (!removedPages.isEmpty()) {
            comicPageRepository.deleteAll(removedPages);
            comicPageRepository.flush();
        }

        List<String> newFilesToRollback = new ArrayList<>();
        try {
            List<ComicPage> finalPages = new ArrayList<>();

            for (int index = 0; index < instructions.size(); index++) {
                int targetPageNumber = index + 1;
                PageInstruction instruction = instructions.get(index);

                if (instruction.pageId() != null) {
                    ComicPage page = existingById.get(instruction.pageId());
                    page.setPageNumber(targetPageNumber);

                    if (StringUtils.hasText(instruction.fileField())) {
                        MultipartFile replacementFile = multipartRequest.getFile(instruction.fileField());
                        validateEditablePageFile(replacementFile);

                        String storedFileName = storePageFile(replacementFile);
                        newFilesToRollback.add(storedFileName);

                        if (StringUtils.hasText(page.getImagePath())) {
                            oldFilesToDelete.add(page.getImagePath());
                        }

                        page.setImagePath(storedFileName);
                    }

                    finalPages.add(page);
                    continue;
                }

                if (!StringUtils.hasText(instruction.fileField())) {
                    throw new IllegalArgumentException("Для новой страницы нужно выбрать изображение.");
                }

                MultipartFile newFile = multipartRequest.getFile(instruction.fileField());
                validateEditablePageFile(newFile);

                String storedFileName = storePageFile(newFile);
                newFilesToRollback.add(storedFileName);

                ComicPage newPage = new ComicPage();
                newPage.setTranslation(translation);
                newPage.setPageNumber(targetPageNumber);
                newPage.setImagePath(storedFileName);
                finalPages.add(newPage);
            }

            comicPageRepository.saveAll(finalPages);
            comicPageRepository.flush();

            deleteStoredFiles(oldFilesToDelete);
        } catch (RuntimeException e) {
            deleteStoredFiles(newFilesToRollback);
            throw e;
        }
    }

    private List<PageInstruction> parseInstructions(String pagesPayload) {
        if (!StringUtils.hasText(pagesPayload)) {
            throw new IllegalArgumentException("Не удалось обработать изменения страниц.");
        }

        try {
            List<PageInstruction> instructions = objectMapper.readValue(
                    pagesPayload,
                    new TypeReference<List<PageInstruction>>() {
                    }
            );

            if (instructions == null || instructions.isEmpty()) {
                throw new IllegalArgumentException("В переводе должна остаться хотя бы одна страница.");
            }

            return instructions.stream()
                    .sorted(Comparator.comparingInt(PageInstruction::order))
                    .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось обработать изменения страниц.");
        }
    }

    private void validateEditablePageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Выберите изображение страницы.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Каждое изображение должно быть не больше 1 МБ.");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        if (!StringUtils.hasText(originalName)) {
            throw new IllegalArgumentException("У файла страницы должно быть корректное имя.");
        }

        String extension = StringUtils.getFilenameExtension(originalName);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!"jpg".equals(normalizedExtension)) {
            throw new IllegalArgumentException("Можно загружать только JPG-файлы.");
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!"image/jpeg".equals(normalizedContentType)) {
            throw new IllegalArgumentException("Можно загружать только JPG-файлы.");
        }
    }


    private String storePageFile(MultipartFile file) {
        String storedFileName = UUID.randomUUID() + ".jpg";

        try {
            uploadStorageService.storePage(file, storedFileName);
            return storedFileName;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить страницы перевода.");
        }
    }

    private void cleanupEmptyChapter(Integer chapterId, Integer comicId) {
        if (chapterId == null || translationRepository.countByChapter_Id(chapterId) > 0) {
            return;
        }

        notificationRepository.detachDeletedChapter(chapterId);
        chapterRepository.deleteById(chapterId);
        chapterRepository.flush();

        Comic comic = comicRepository.findById(comicId).orElse(null);
        if (comic != null) {
            comic.setChaptersCount((int) chapterRepository.countByComic_Id(comicId));
            comicRepository.save(comic);
        }
    }

    private void deleteStoredFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            if (!StringUtils.hasText(fileName)) {
                continue;
            }

            uploadStorageService.deletePageIfExists(fileName);
        }
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Название перевода обязательно.");
        }
        return normalized.length() > MAX_TITLE_LENGTH
                ? normalized.substring(0, MAX_TITLE_LENGTH)
                : normalized;
    }

    private void requireAdmin(User admin) {
        if (admin == null || admin.getRole() == null || !ADMIN_ROLE.equalsIgnoreCase(admin.getRole().getName())) {
            throw new IllegalStateException("Недостаточно прав.");
        }
    }

    private record PageInstruction(Integer pageId, String fileField, int order) {
    }
}
