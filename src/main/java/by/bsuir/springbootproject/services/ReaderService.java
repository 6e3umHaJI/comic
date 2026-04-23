package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.ContinueReadingInfo;
import by.bsuir.springbootproject.dto.ReaderData;

import java.util.List;
import java.util.Set;

public interface ReaderService {

    default ReaderData getReaderData(Integer translationId) {
        return getReaderData(translationId, false);
    }

    ReaderData getReaderData(Integer translationId, boolean allowUnapprovedPreview);

    void markChapterReadIfAuthenticated(Integer chapterId);

    void markTranslationOpenedIfAuthenticated(Integer translationId);

    Integer getSavedPageIfAuthenticated(Integer translationId);

    void saveProgressIfAuthenticated(Integer translationId, Integer page);

    Integer getFirstAvailableTranslationId(Integer comicId);

    ContinueReadingInfo getContinueReadingInfoIfAuthenticated(Integer comicId);

    Set<Integer> getReadTranslationIdsIfAuthenticated(List<Integer> translationIds);

    List<String> getApprovedLanguagesByChapterId(Integer chapterId);
}
