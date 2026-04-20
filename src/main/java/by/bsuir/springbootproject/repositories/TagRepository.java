package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    List<Tag> findAllByOrderByNameAsc();

    Optional<Tag> findByNameIgnoreCase(String name);
}
