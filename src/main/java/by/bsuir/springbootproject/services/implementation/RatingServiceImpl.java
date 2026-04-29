package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Rating;
import by.bsuir.springbootproject.entities.RatingScore;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.RatingRepository;
import by.bsuir.springbootproject.repositories.RatingScoreRepository;
import by.bsuir.springbootproject.services.RatingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingScoreRepository scoreRepository;
    private final ComicRepository comicRepository;

    @Override
    public Rating rateComic(User user, int comicId, int value) {
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Авторизуйтесь, чтобы поставить оценку.");
        }

        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }

        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new RuntimeException("Комикс не найден"));

        RatingScore score = scoreRepository.findByValue((short) value)
                .orElseThrow(() -> new RuntimeException("Неверное значение рейтинга"));

        ratingRepository.upsertRating(user.getId(), comic.getId(), score.getId());

        return ratingRepository.findByUserAndComic(user, comic)
                .orElseThrow(() -> new IllegalStateException("Не удалось сохранить оценку."));
    }

    @Override
    public void removeRating(User user, int comicId) {
        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new RuntimeException("Комикс не найден"));

        ratingRepository.findByUserAndComic(user, comic)
                .ifPresent(ratingRepository::delete);
    }
}