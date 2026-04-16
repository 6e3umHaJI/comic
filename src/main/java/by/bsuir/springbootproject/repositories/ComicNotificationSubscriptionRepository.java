package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicNotificationSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComicNotificationSubscriptionRepository extends JpaRepository<ComicNotificationSubscription, Integer> {

    Optional<ComicNotificationSubscription> findByUser_IdAndComic_Id(Integer userId, Integer comicId);

    boolean existsByUser_IdAndComic_Id(Integer userId, Integer comicId);

    void deleteByUser_IdAndComic_Id(Integer userId, Integer comicId);

    @EntityGraph(attributePaths = {"comic"})
    @Query(
            value = """
                    select s
                    from ComicNotificationSubscription s
                    join s.comic c
                    where s.user.id = :userId
                      and (
                          :q = ''
                          or lower(c.title) like lower(concat('%', :q, '%'))
                          or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                      )
                    order by s.createdAt desc, c.id desc
                    """,
            countQuery = """
                    select count(s)
                    from ComicNotificationSubscription s
                    join s.comic c
                    where s.user.id = :userId
                      and (
                          :q = ''
                          or lower(c.title) like lower(concat('%', :q, '%'))
                          or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                      )
                    """
    )
    Page<ComicNotificationSubscription> findPageByUserIdAndQuery(Integer userId, String q, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comic"})
    List<ComicNotificationSubscription> findByComic_Id(Integer comicId);
}
