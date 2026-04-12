package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.ReaderData;

public interface ReaderService {
    ReaderData getReaderData(Integer translationId);
    void markChapterReadIfAuthenticated(Integer chapterId);
    Integer getSavedPageIfAuthenticated(Integer translationId);
    void saveProgressIfAuthenticated(Integer translationId, Integer page);
}