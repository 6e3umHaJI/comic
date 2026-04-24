package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.User;

public interface AdminComicDeletionService {
    void deleteComic(Integer comicId, User admin);
}