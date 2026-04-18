package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.RecentUpdate;
import by.bsuir.springbootproject.dto.RelatedWithType;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;

import java.util.List;
import java.util.Map;

public interface ComicService {
    List<Comic> getMostPopularComics();

    List<Comic> getNewestComics();

    Comic getComicById(Integer id);

    List<Comic> getRelatedComics(Integer id);

    List<Comic> getSimilarComics(Integer id);

    List<Chapter> getChapters(Integer comicId);

    Map<Integer, Long> getRatingDistribution(int comicId);

    Map<String, Long> getFavoriteStats(int comicId);

    List<Object[]> getApprovedLangStatsByComic(int comicId);

    Map<Integer, List<String>> getApprovedLangsByChapter(int comicId);

    long getRelatedComicsCount(int id);

    List<RelatedWithType> getRelatedComicsWithTypePaged(int id, int page, int size);

    List<RecentUpdate> getRecentUpdates();

    Map<String, Object> getStartReadingData(Integer comicId);

    Map<String, Object> getChaptersChunkData(Integer comicId, int page, Integer size, String dir, String q);

    Map<String, Object> getTranslationsChunkData(Integer chapterId, String lang, int page, int size);
}
