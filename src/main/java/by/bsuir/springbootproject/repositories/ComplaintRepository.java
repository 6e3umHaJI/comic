package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {
    long countByUser_IdAndStatus_Name(Integer userId, String statusName);
}
