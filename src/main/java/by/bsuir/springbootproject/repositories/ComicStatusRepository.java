package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComicStatusRepository extends JpaRepository<ComicStatus, Integer>{
}