package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.SavedComic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavedComicRepository extends JpaRepository<SavedComic, Integer> {

    boolean existsBySectionIdAndComicId(Integer sectionId, Integer comicId);

    boolean existsBySectionUserIdAndComicId(Integer userId, Integer comicId);

    Optional<SavedComic> findBySectionIdAndComicId(Integer sectionId, Integer comicId);

    @Modifying
    @Query("""
        delete from SavedComic sc
        where sc.section.id = :sectionId and sc.comic.id = :comicId
    """)
    void deleteBySectionIdAndComicId(@Param("sectionId") Integer sectionId,
                                     @Param("comicId") Integer comicId);

    @Modifying
    @Query("""
        delete from SavedComic sc
        where sc.section.id = :sectionId and sc.comic.id in :comicIds
    """)
    void deleteBySectionIdAndComicIds(@Param("sectionId") Integer sectionId,
                                      @Param("comicIds") List<Integer> comicIds);

    @Modifying
    @Query(
            value = """
            insert into saved_comics (section_id, comic_id, added_at)
            values (:sectionId, :comicId, :addedAt)
        """,
            nativeQuery = true
    )
    void insertSavedComic(@Param("sectionId") Integer sectionId,
                          @Param("comicId") Integer comicId,
                          @Param("addedAt") LocalDateTime addedAt);

    @Query("""
        select sc.section.id
        from SavedComic sc
        where sc.section.user.id = :userId and sc.comic.id = :comicId
    """)
    List<Integer> findSectionIdsByUserIdAndComicId(Integer userId, Integer comicId);

    @Query("""
        select sc
        from SavedComic sc
        join fetch sc.comic c
        where sc.section.id = :sectionId
    """)
    List<SavedComic> findAllBySectionId(Integer sectionId);

    @Query(
            value = """
            select sc
            from SavedComic sc
            join fetch sc.comic c
            left join fetch c.type
            left join fetch c.ageRating
            left join fetch c.translationStatus
            left join fetch c.comicStatus
            where sc.section.id = :sectionId
        """,
            countQuery = """
            select count(sc)
            from SavedComic sc
            where sc.section.id = :sectionId
        """
    )
    Page<SavedComic> findPageBySectionId(Integer sectionId, Pageable pageable);
}
