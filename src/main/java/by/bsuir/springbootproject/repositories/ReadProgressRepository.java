package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ReadProgress;
import by.bsuir.springbootproject.entities.ReadProgressId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadProgressRepository extends JpaRepository<ReadProgress, ReadProgressId> {

    Optional<ReadProgress> findByUser_IdAndTranslation_Id(Integer userId, Integer translationId);

    @EntityGraph(attributePaths = {"translation"})
    List<ReadProgress> findByUser_IdAndTranslation_IdIn(Integer userId, List<Integer> translationIds);

    @Query("""
            select rp
            from ReadProgress rp
            join fetch rp.translation t
            join t.chapter ch
            where rp.user.id = :userId
              and ch.comic.id = :comicId
            order by rp.updatedAt desc, t.id desc
            """)
    List<ReadProgress> findLatestByUserIdAndComicId(Integer userId, Integer comicId, Pageable pageable);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from ReadProgress rp
            where rp.translation.chapter.comic.id = :comicId
            """)
    void deleteAllByComicId(@Param("comicId") Integer comicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ReadProgress rp
        where rp.translation.id = :translationId
        """)
    void deleteByTranslationId(@Param("translationId") Integer translationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.chapter = null,
            n.linkPath = null,
            n.isClickable = false
        where n.chapter.id = :chapterId
        """)
    void detachDeletedChapter(@Param("chapterId") Integer chapterId);
}
