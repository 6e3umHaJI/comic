package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComplaintType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintTypeRepository extends JpaRepository<ComplaintType, Integer> {
    List<ComplaintType> findByScopeOrderByIdAsc(String scope);
}
