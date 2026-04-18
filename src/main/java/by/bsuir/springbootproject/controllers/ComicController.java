package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.CollectionService;
import by.bsuir.springbootproject.services.ComicService;
import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import by.bsuir.springbootproject.services.ReaderService;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/comics")
public class ComicController {

    private static final String TAB_DESCRIPTION = "description";
    private static final String TAB_CHAPTERS = "chapters";
    private static final String SORT_DIRECTION_DESC = "desc";
    private static final String SORT_FIELD_CHAPTER_NUMBER = "chapterNumber";
    private static final String SORT_FIELD_CREATED_AT = "createdAt";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final String EMPTY_STRING = "";

    private final ComicService comicService;
    private final CollectionService collectionService;
    private final ChapterRepository chapterRepository;
    private final TranslationRepository translationRepository;
    private final SecurityContextUtils securityContextUtils;
    private final ReaderService readerService;
    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public String showComic(@PathVariable Integer id,
                            @RequestParam(defaultValue = TAB_DESCRIPTION) String tab,
                            HttpServletRequest request,
                            Model model) {
        Comic comic = comicService.getComicById(id);
        model.addAttribute("comic", comic);
        model.addAttribute("tab", tab);
        model.addAttribute("isLogged", request.getUserPrincipal() != null);

        model.addAttribute("chaptersPageSize", Values.CHAPTERS_PAGE_SIZE);
        Translation firstApprovedTranslation = translationRepository.findFirstApprovedByComic(id, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        model.addAttribute("startReadingTranslationId", firstApprovedTranslation != null ? firstApprovedTranslation.getId() : null);
        model.addAttribute("continueReading", readerService.getContinueReadingInfoIfAuthenticated(id));

        if (firstApprovedTranslation != null) {
            Integer startReadingChapterId = firstApprovedTranslation.getChapter().getId();
            Integer startReadingChapterNumber = firstApprovedTranslation.getChapter().getChapterNumber();

            String startReadingLangsCsv = translationRepository.findApprovedLangsByChapterIds(List.of(startReadingChapterId))
                    .stream()
                    .filter(row -> row[0] != null && ((Number) row[0]).intValue() == startReadingChapterId)
                    .map(row -> (String) row[1])
                    .filter(Objects::nonNull)
                    .distinct()
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");

            model.addAttribute("startReadingChapterId", startReadingChapterId);
            model.addAttribute("startReadingChapterNumber", startReadingChapterNumber);
            model.addAttribute("startReadingLangsCsv", startReadingLangsCsv);
        } else {
            model.addAttribute("startReadingChapterId", null);
            model.addAttribute("startReadingChapterNumber", null);
            model.addAttribute("startReadingLangsCsv", "");
        }

        if (request.getUserPrincipal() != null) {
            Integer userId = securityContextUtils.getUserFromContext()
                    .map(by.bsuir.springbootproject.entities.User::getId)
                    .orElse(null);

            if (userId != null) {
                boolean inCollections = collectionService.isComicInCollections(userId, id);
                boolean isNotificationsEnabled = notificationService.isComicSubscribed(userId, id);

                model.addAttribute("inCollections", inCollections);
                model.addAttribute("isNotificationsEnabled", isNotificationsEnabled);
            } else {
                model.addAttribute("inCollections", false);
                model.addAttribute("isNotificationsEnabled", false);
            }
        } else {
            model.addAttribute("inCollections", false);
            model.addAttribute("isNotificationsEnabled", false);
        }

        if (!TAB_CHAPTERS.equals(tab)) {
            model.addAttribute("relatedComics", comicService.getRelatedComics(id));
            model.addAttribute("relatedCount", comicService.getRelatedComicsCount(id));
            model.addAttribute("similarComics", comicService.getSimilarComics(id));
            model.addAttribute("ratingDistribution", comicService.getRatingDistribution(id));
            model.addAttribute("favoriteStats", comicService.getFavoriteStats(id));
            model.addAttribute("approvedLangStats", comicService.getApprovedLangStatsByComic(id));
        }

        boolean ajax = XML_HTTP_REQUEST.equals(request.getHeader("X-Requested-With"));
        return ajax ? "comic/comic-tab-content" : "comic/comic-page";
    }

    @GetMapping("/{id}/live-stats")
    @ResponseBody
    public Map<String, Object> getLiveStats(@PathVariable Integer id) {
        Comic comic = comicService.getComicById(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("avgRating", comic.getAvgRating() != null ? comic.getAvgRating() : 0);
        response.put("ratingsCount", comic.getRatingsCount() != null ? comic.getRatingsCount() : 0L);
        response.put("ratingDistribution", comicService.getRatingDistribution(id));
        response.put("favoriteStats", comicService.getFavoriteStats(id));

        return response;
    }



    @GetMapping("/{id}/related")
    public String getRelatedPaged(@PathVariable Integer id,
                                  @RequestParam(defaultValue = "0") int page,
                                  Model model) {
        model.addAttribute("related", comicService.getRelatedComicsWithTypePaged(id, page, Values.RELATED_PAGE_SIZE));
        return "comic/related-list";
    }

    @GetMapping("/{comicId}/chapters")
    public String getChaptersPageChunk(@PathVariable Integer comicId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestParam(defaultValue = SORT_DIRECTION_DESC) String dir,
                                       @RequestParam(defaultValue = EMPTY_STRING) String q,
                                       Model model) {
        int pageSize = (size == null || size <= 0) ? Values.CHAPTERS_PAGE_SIZE : size;

        Sort sort = Sort.by(SORT_FIELD_CHAPTER_NUMBER);
        sort = SORT_DIRECTION_DESC.equalsIgnoreCase(dir) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<Chapter> chapterPage = chapterRepository.searchByComicIdAndQuery(comicId, q.trim(), pageable);

        List<Integer> ids = chapterPage.getContent().stream().map(Chapter::getId).toList();
        Map<Integer, List<String>> langsByChapter = fetchApprovedLangsByChapterIds(ids);

        Map<Integer, String> langsCsvByChapter = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : langsByChapter.entrySet()) {
            langsCsvByChapter.put(e.getKey(), String.join(",", e.getValue()));
        }

        int totalPages = chapterPage.getTotalPages();
        int currentPage = chapterPage.getNumber() + 1;

        int visiblePages = 5;
        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(beginPage + visiblePages - 1, totalPages);

        if (endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        boolean showLeftDots = beginPage > 2;
        boolean showRightDots = endPage < totalPages - 1;

        model.addAttribute("chapters", chapterPage.getContent());
        model.addAttribute("langsByChapter", langsByChapter);
        model.addAttribute("langsCsvByChapter", langsCsvByChapter);

        model.addAttribute("comicId", comicId);
        model.addAttribute("total", chapterPage.getTotalElements());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", page);
        model.addAttribute("size", pageSize);
        model.addAttribute("dir", dir);
        model.addAttribute("q", q);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("beginPage", beginPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showLeftDots", showLeftDots);
        model.addAttribute("showRightDots", showRightDots);

        return "comic/chapter-list";
    }

    private Map<Integer, List<String>> fetchApprovedLangsByChapterIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = translationRepository.findApprovedLangsByChapterIds(ids);
        Map<Integer, List<String>> map = new HashMap<>();

        for (Object[] r : rows) {
            Integer chId = ((Number) r[0]).intValue();
            String lang = (String) r[1];
            map.computeIfAbsent(chId, k -> new ArrayList<>()).add(lang);
        }

        return map;
    }

    @GetMapping("/chapters/{chapterId}/translations")
    public String getTranslationsChunk(@PathVariable Integer chapterId,
                                       @RequestParam(defaultValue = EMPTY_STRING) String lang,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       Model model) {
        int pageSize = (size <= 0) ? Values.TRANSLATIONS_PAGE_SIZE : size;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(SORT_FIELD_CREATED_AT).descending());

        Page<Translation> tPage = (lang == null || lang.isBlank())
                ? translationRepository.findApprovedByChapterId(chapterId, pageable)
                : translationRepository.findApprovedByChapterIdAndLang(chapterId, lang.trim(), pageable);

        List<Integer> translationIds = tPage.getContent().stream()
                .map(Translation::getId)
                .toList();

        model.addAttribute("translations", tPage.getContent());
        model.addAttribute("readTranslationIds", readerService.getReadTranslationIdsIfAuthenticated(translationIds));
        model.addAttribute("total", tPage.getTotalElements());
        model.addAttribute("totalPages", tPage.getTotalPages());
        model.addAttribute("page", page);
        model.addAttribute("size", pageSize);
        model.addAttribute("lang", lang);
        model.addAttribute("chapterId", chapterId);

        return "comic/translation-items";
    }

}