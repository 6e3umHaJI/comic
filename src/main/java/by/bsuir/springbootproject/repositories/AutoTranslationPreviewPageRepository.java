package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.AutoTranslationPreviewPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoTranslationPreviewPageRepository extends JpaRepository<AutoTranslationPreviewPage, Integer> {
    List<AutoTranslationPreviewPage> findByPreview_IdOrderByPageNumberAsc(Integer previewId);
}
