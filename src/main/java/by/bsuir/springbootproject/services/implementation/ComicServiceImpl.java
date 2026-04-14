package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.RecentUpdate;
import by.bsuir.springbootproject.dto.RelatedWithType;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("Комикс не найден: " + id));
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
                .sorted((a, b) -> a.getChapterNumber().compareTo(b.getChapterNumber()))
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
    public Map<Integer, List<String>> getApprovedLangsByChapter(int comicId) {
        return translationRepository.findApprovedLangsByChapterIds(
                        getChapters(comicId).stream().map(Chapter::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(
                        row -> (Integer) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));
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
}
