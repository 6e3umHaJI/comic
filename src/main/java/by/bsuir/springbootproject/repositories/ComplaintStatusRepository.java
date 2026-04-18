package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintStatusRepository extends JpaRepository<ComplaintStatus, Integer> {
    Optional<ComplaintStatus> findByName(String name);
}