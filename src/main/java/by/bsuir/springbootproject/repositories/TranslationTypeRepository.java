package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.TranslationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranslationTypeRepository extends JpaRepository<TranslationType, Integer> {
    Optional<TranslationType> findByName(String name);
}
