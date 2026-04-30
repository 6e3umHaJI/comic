package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.dto.RecentUpdate;
import by.bsuir.springbootproject.dto.RelatedWithType;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComicServiceImpl implements ComicService {

    private static final Sort POPULAR_SORT = Sort.by(
            Sort.Order.desc("popularityScore"),
            Sort.Order.desc("id")
    );

    private static final Sort NEWEST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private static final List<String> COLLECTION_BUCKETS = List.of(
            "Читаю",
            "В планах",
            "Прочитано",
            "Другие"
    );

    private final ComicRepository comicRepository;
    private final TranslationRepository translationRepository;
    private final ChapterRepository chapterRepository;

    @Override
    public List<Comic> getMostPopularComics() {
        return comicRepository.findAll(PageRequest.of(0, 10, POPULAR_SORT)).getContent();
    }

    @Override
    public List<RecentUpdate> getRecentUpdates() {
        return translationRepository.findRecentApprovedTranslations(PageRequest.of(0, 10))
                .stream()
                .map(t -> new RecentUpdate(
                        t.getId(),
                        t.getChapter().getComic().getId(),
                        t.getChapter().getComic().getTitle(),
                        t.getChapter().getComic().getCover(),
                        t.getChapter().getChapterNumber(),
                        t.getLanguage().getName(),
                        t.getCreatedAtFormatted(),
                        t.getCreatedAtIso()
                ))
                .toList();
    }

    @Override
    public List<Comic> getNewestComics() {
        return comicRepository.findAll(PageRequest.of(0, 10, NEWEST_SORT)).getContent();
    }

    @Override
    public Comic getComicById(Integer id) {
        return comicRepository.findByIdForComicPage(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Комикс не найден"
                ));
    }

    @Override
    public List<Comic> getRelatedComics(Integer id) {
        return comicRepository.findRelatedComics(id);
    }

    @Override
    public List<Comic> getSimilarComics(Integer id) {
        return comicRepository.findSimilarComics(id).stream()
                .limit(6)
                .toList();
    }

    @Override
    public List<Chapter> getChapters(Integer comicId) {
        Comic comic = getComicById(comicId);
        return comic.getChapters().stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber))
                .toList();
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(int comicId) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        result.put(5, 0L);
        result.put(4, 0L);
        result.put(3, 0L);
        result.put(2, 0L);
        result.put(1, 0L);

        comicRepository.getRatingDistribution(comicId).forEach(row -> {
            Integer value = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            result.put(value, count);
        });

        return result;
    }

    @Override
    public Map<String, Long> getFavoriteStats(int comicId) {
        Map<String, Long> result = new LinkedHashMap<>();
        COLLECTION_BUCKETS.forEach(name -> result.put(name, 0L));

        comicRepository.getFavoriteStats(comicId).forEach(row -> {
            String name = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            result.put(name, count);
        });

        return result;
    }

    @Override
    public List<Object[]> getApprovedLangStatsByComic(int comicId) {
        return comicRepository.getApprovedLangStatsByComic(comicId);
    }

    @Override
    public long getRelatedComicsCount(int id) {
        return comicRepository.countRelatedByComic(id);
    }

    @Override
    public List<RelatedWithType> getRelatedComicsWithTypePaged(int id, int page, int size) {
        List<Object[]> rows = comicRepository.findRelatedComicsWithTypes(id);

        Map<Integer, Comic> comicsById = new LinkedHashMap<>();
        Map<Integer, LinkedHashSet<String>> typesByComicId = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Comic comic = (Comic) row[0];
            String relationType = (String) row[1];

            comicsById.putIfAbsent(comic.getId(), comic);
            typesByComicId.computeIfAbsent(comic.getId(), k -> new LinkedHashSet<>());

            if (relationType != null && !relationType.isBlank()) {
                typesByComicId.get(comic.getId()).add(relationType);
            }
        }

        return comicsById.entrySet().stream()
                .skip((long) page * size)
                .limit(size)
                .map(entry -> new RelatedWithType(
                        entry.getValue(),
                        String.join(", ", typesByComicId.getOrDefault(entry.getKey(), new LinkedHashSet<>()))
                ))
                .toList();
    }

    @Override
    public Map<String, Object> getStartReadingData(Integer comicId) {
        Map<String, Object> result = new HashMap<>();

        Translation firstApprovedTranslation = translationRepository.findFirstApprovedByComic(comicId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        result.put("startReadingTranslationId", firstApprovedTranslation != null ? firstApprovedTranslation.getId() : null);

        if (firstApprovedTranslation == null) {
            result.put("startReadingChapterId", null);
            result.put("startReadingChapterNumber", null);
            result.put("startReadingLangsCsv", "");
            return result;
        }

        Integer startReadingChapterId = firstApprovedTranslation.getChapter().getId();
        Integer startReadingChapterNumber = firstApprovedTranslation.getChapter().getChapterNumber();

        String startReadingLangsCsv = translationRepository.findApprovedLangsByChapterIds(List.of(startReadingChapterId))
                .stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == startReadingChapterId)
                .map(row -> (String) row[1])
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        result.put("startReadingChapterId", startReadingChapterId);
        result.put("startReadingChapterNumber", startReadingChapterNumber);
        result.put("startReadingLangsCsv", startReadingLangsCsv);

        return result;
    }

    @Override
    public Map<String, Object> getChaptersChunkData(Integer comicId, int page, Integer size, String dir, String q) {
        Map<String, Object> result = new HashMap<>();

        int pageSize = (size == null || size <= 0) ? Values.CHAPTERS_PAGE_SIZE : size;
        String query = q == null ? "" : q.trim();

        Sort sort = Sort.by("chapterNumber");
        sort = "desc".equalsIgnoreCase(dir) ? sort.descending() : sort.ascending();

        Pageable pageable = PageRequest.of(page, pageSize, sort);
        Page<Chapter> chapterPage = chapterRepository.searchByComicIdAndQuery(comicId, query, pageable);

        List<Integer> ids = chapterPage.getContent().stream()
                .map(Chapter::getId)
                .toList();

        Map<Integer, List<String>> langsByChapter = fetchApprovedLangsByChapterIds(ids);
        Map<Integer, String> langsCsvByChapter = new HashMap<>();

        for (Map.Entry<Integer, List<String>> entry : langsByChapter.entrySet()) {
            langsCsvByChapter.put(entry.getKey(), String.join(",", entry.getValue()));
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

        result.put("chapters", chapterPage.getContent());
        result.put("langsByChapter", langsByChapter);
        result.put("langsCsvByChapter", langsCsvByChapter);
        result.put("comicId", comicId);
        result.put("total", chapterPage.getTotalElements());
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", pageSize);
        result.put("dir", dir);
        result.put("q", query);
        result.put("currentPage", currentPage);
        result.put("beginPage", beginPage);
        result.put("endPage", endPage);
        result.put("showLeftDots", showLeftDots);
        result.put("showRightDots", showRightDots);

        return result;
    }

    @Override
    public Map<String, Object> getTranslationsChunkData(Integer chapterId, String lang, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        int pageSize = size <= 0 ? Values.TRANSLATIONS_PAGE_SIZE : size;
        String actualLang = lang == null ? "" : lang.trim();

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());

        Page<Translation> translationPage = actualLang.isBlank()
                ? translationRepository.findApprovedByChapterId(chapterId, pageable)
                : translationRepository.findApprovedByChapterIdAndLang(chapterId, actualLang, pageable);

        List<Integer> translationIds = translationPage.getContent().stream()
                .map(Translation::getId)
                .toList();

        result.put("translations", translationPage.getContent());
        result.put("translationIds", translationIds);
        result.put("total", translationPage.getTotalElements());
        result.put("totalPages", translationPage.getTotalPages());
        result.put("page", page);
        result.put("size", pageSize);
        result.put("lang", actualLang);
        result.put("chapterId", chapterId);

        return result;
    }

    private Map<Integer, List<String>> fetchApprovedLangsByChapterIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = translationRepository.findApprovedLangsByChapterIds(ids);
        Map<Integer, List<String>> map = new HashMap<>();

        for (Object[] row : rows) {
            Integer chapterId = ((Number) row[0]).intValue();
            String lang = (String) row[1];
            map.computeIfAbsent(chapterId, key -> new ArrayList<>()).add(lang);
        }

        return map;
    }
}
