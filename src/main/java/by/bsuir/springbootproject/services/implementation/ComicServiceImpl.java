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
        return comicRepository.getRatingDistribution(comicId).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    @Override
    public Map<String, Long> getFavoriteStats(int comicId) {
        return comicRepository.getFavoriteStats(comicId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
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
        Pageable pageable = PageRequest.of(page, size);

        return comicRepository.findRelatedComicsWithTypePaged(id, pageable)
                .getContent()
                .stream()
                .map(row -> new RelatedWithType((Comic) row[0], (String) row[1]))
                .toList();
    }
}