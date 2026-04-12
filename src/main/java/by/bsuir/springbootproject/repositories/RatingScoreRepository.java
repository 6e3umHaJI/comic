package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.RatingScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RatingScoreRepository extends JpaRepository<RatingScore, Integer> {
    Optional<RatingScore> findByValue(short value);
}