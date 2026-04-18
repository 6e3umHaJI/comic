package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {

    long countByUser_IdAndStatus_NameIn(Integer userId, Collection<String> statusNames);

    @EntityGraph(attributePaths = {"user", "type", "status"})
    @Query(
            value = """
                    select c
                    from Complaint c
                    join c.type t
                    join c.status s
                    where t.scope = :scope
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                    """,
            countQuery = """
                    select count(c)
                    from Complaint c
                    join c.type t
                    join c.status s
                    where t.scope = :scope
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                    """
    )
    Page<Complaint> findAdminComplaints(String scope,
                                        Integer typeId,
                                        Collection<String> visibleStatuses,
                                        Pageable pageable);
}
