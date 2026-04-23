package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewStatusRepository extends JpaRepository<ReviewStatus, Integer> {
    Optional<ReviewStatus> findByName(String name);
}
