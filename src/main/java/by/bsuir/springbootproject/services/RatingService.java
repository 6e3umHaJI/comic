package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Rating;
import by.bsuir.springbootproject.entities.User;

public interface RatingService {
    Rating rateComic(User user, int comicId, int value);
    void removeRating(User user, int comicId);
}