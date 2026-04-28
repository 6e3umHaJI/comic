package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.ContinueReadingInfo;
import by.bsuir.springbootproject.dto.ReaderData;

import java.util.List;
import java.util.Set;

public interface ReaderService {

    ReaderData getReaderData(Integer translationId, boolean allowUnapprovedPreview);

    void markTranslationOpenedIfAuthenticated(Integer translationId);

    Integer getSavedPageIfAuthenticated(Integer translationId);

    void saveProgressIfAuthenticated(Integer translationId, Integer page);

    ContinueReadingInfo getContinueReadingInfoIfAuthenticated(Integer comicId);

    Set<Integer> getReadTranslationIdsIfAuthenticated(List<Integer> translationIds);

    List<String> getApprovedLanguagesByChapterId(Integer chapterId);
}
