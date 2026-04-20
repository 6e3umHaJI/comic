package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    List<Genre> findAllByOrderByNameAsc();

    Optional<Genre> findByNameIgnoreCase(String name);
}
