package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {

    List<Language> findAllByOrderByNameAsc();

    List<Language> findAllByTranslationCodeIsNotNullOrderByNameAsc();

    List<Language> findAllByOcrSpaceCodeIsNotNullAndTranslationCodeIsNotNullOrderByNameAsc();
}
