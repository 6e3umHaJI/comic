package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.AgeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgeRatingRepository extends JpaRepository<AgeRating, Integer> {
    List<AgeRating> findAllByOrderByIdAsc();
}
