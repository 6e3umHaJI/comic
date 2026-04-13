package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer>{
}