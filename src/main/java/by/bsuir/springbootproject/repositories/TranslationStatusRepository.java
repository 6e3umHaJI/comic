package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.TranslationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationStatusRepository extends JpaRepository<TranslationStatus, Integer>{
}