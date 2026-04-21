package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicNotificationSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicNotificationSubscriptionRepository extends JpaRepository<ComicNotificationSubscription, Integer> {

    boolean existsByUser_IdAndComic_Id(Integer userId, Integer comicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ComicNotificationSubscription s where s.user.id = :userId and s.comic.id = :comicId")
    int deleteByUserIdAndComicId(@Param("userId") Integer userId,
                                 @Param("comicId") Integer comicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    insert into comic_notification_subscriptions (user_id, comic_id, created_at)
                    values (:userId, :comicId, current_timestamp)
                    on conflict (user_id, comic_id) do nothing
                    """,
            nativeQuery = true
    )
    int insertIgnore(@Param("userId") Integer userId,
                     @Param("comicId") Integer comicId);

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
    Page<ComicNotificationSubscription> findPageByUserIdAndQuery(@Param("userId") Integer userId,
                                                                 @Param("q") String q,
                                                                 Pageable pageable);

    @EntityGraph(attributePaths = {"user", "comic"})
    List<ComicNotificationSubscription> findByComic_Id(Integer comicId);
}
