package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationTypeRepository extends JpaRepository<RelationType, Integer> {
    List<RelationType> findAllByOrderByNameAsc();

    Optional<RelationType> findByNameIgnoreCase(String name);
}
