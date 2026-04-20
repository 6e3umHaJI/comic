package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicTypeRepository extends JpaRepository<ComicType, Integer> {
    List<ComicType> findAllByOrderByNameAsc();
}
