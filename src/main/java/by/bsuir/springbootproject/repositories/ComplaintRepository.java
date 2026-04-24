package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
                    where t.scope = 'COMIC'
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                      and (
                           :q = ''
                           or exists (
                               select 1
                               from Comic cm
                               where cm.id = c.targetId
                                 and (
                                      lower(cm.title) like lower(concat('%', :q, '%'))
                                      or lower(cm.originalTitle) like lower(concat('%', :q, '%'))
                                 )
                           )
                      )
                    """,
            countQuery = """
                    select count(c)
                    from Complaint c
                    join c.type t
                    join c.status s
                    where t.scope = 'COMIC'
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                      and (
                           :q = ''
                           or exists (
                               select 1
                               from Comic cm
                               where cm.id = c.targetId
                                 and (
                                      lower(cm.title) like lower(concat('%', :q, '%'))
                                      or lower(cm.originalTitle) like lower(concat('%', :q, '%'))
                                 )
                           )
                      )
                    """
    )
    Page<Complaint> findAdminComicComplaints(@Param("typeId") Integer typeId,
                                             @Param("q") String q,
                                             @Param("visibleStatuses") Collection<String> visibleStatuses,
                                             Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type", "status"})
    @Query(
            value = """
                    select c
                    from Complaint c
                    join c.type t
                    join c.status s
                    where t.scope = 'TRANSLATION'
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                      and (
                           :q = ''
                           or exists (
                               select 1
                               from Translation tr
                               join tr.chapter ch
                               join ch.comic cm
                               where tr.id = c.targetId
                                 and (
                                      lower(cm.title) like lower(concat('%', :q, '%'))
                                      or lower(cm.originalTitle) like lower(concat('%', :q, '%'))
                                 )
                           )
                      )
                    """,
            countQuery = """
                    select count(c)
                    from Complaint c
                    join c.type t
                    join c.status s
                    where t.scope = 'TRANSLATION'
                      and s.name in :visibleStatuses
                      and (:typeId is null or t.id = :typeId)
                      and (
                           :q = ''
                           or exists (
                               select 1
                               from Translation tr
                               join tr.chapter ch
                               join ch.comic cm
                               where tr.id = c.targetId
                                 and (
                                      lower(cm.title) like lower(concat('%', :q, '%'))
                                      or lower(cm.originalTitle) like lower(concat('%', :q, '%'))
                                 )
                           )
                      )
                    """
    )
    Page<Complaint> findAdminTranslationComplaints(@Param("typeId") Integer typeId,
                                                   @Param("q") String q,
                                                   @Param("visibleStatuses") Collection<String> visibleStatuses,
                                                   Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Complaint c
            where c.targetId = :targetId
              and upper(c.type.scope) = upper(:scope)
            """)
    void deleteByTargetIdAndScope(@Param("targetId") Integer targetId,
                                  @Param("scope") String scope);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Complaint c
            where c.targetId in :targetIds
              and upper(c.type.scope) = upper(:scope)
            """)
    void deleteByTargetIdsAndScope(@Param("targetIds") Collection<Integer> targetIds,
                                   @Param("scope") String scope);
}
