package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.AutoTranslationPreview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoTranslationPreviewRepository extends JpaRepository<AutoTranslationPreview, Integer> {

    @EntityGraph(attributePaths = {"adminUser", "comic", "sourceLanguage", "targetLanguage"})
    Optional<AutoTranslationPreview> findByToken(String token);
}
