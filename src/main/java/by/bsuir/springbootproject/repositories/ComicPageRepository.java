package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicPageRepository extends JpaRepository<ComicPage, Integer> {
    List<ComicPage> findByTranslationIdOrderByPageNumberAsc(Integer translationId);
}