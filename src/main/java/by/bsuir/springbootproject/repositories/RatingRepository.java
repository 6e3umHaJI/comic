package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Rating;
import by.bsuir.springbootproject.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {
    Optional<Rating> findByUserAndComic(User user, Comic comic);
}