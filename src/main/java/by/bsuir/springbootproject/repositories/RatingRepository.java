package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Rating;
import by.bsuir.springbootproject.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {

    Optional<Rating> findByUserAndComic(User user, Comic comic);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Rating r
            where r.comic.id = :comicId
            """)
    void deleteAllByComicId(@Param("comicId") Integer comicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                insert into ratings (user_id, comic_id, score_id)
                values (:userId, :comicId, :scoreId)
                on conflict (user_id, comic_id)
                do update set score_id = excluded.score_id
                """,
            nativeQuery = true
    )
    void upsertRating(@Param("userId") Integer userId,
                      @Param("comicId") Integer comicId,
                      @Param("scoreId") Integer scoreId);
}
