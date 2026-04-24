package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.SavedComic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedComicRepository extends JpaRepository<SavedComic, Integer> {

    boolean existsBySectionIdAndComicId(Integer sectionId, Integer comicId);

    boolean existsBySectionUserIdAndComicId(Integer userId, Integer comicId);

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
    List<Integer> findSectionIdsByUserIdAndComicId(@Param("userId") Integer userId,
                                                   @Param("comicId") Integer comicId);

    @Query("""
            select sc
            from SavedComic sc
            join fetch sc.comic c
            where sc.section.id = :sectionId
            """)
    List<SavedComic> findAllBySectionId(@Param("sectionId") Integer sectionId);

    @EntityGraph(attributePaths = {"section", "section.user", "comic"})
    List<SavedComic> findByComic_Id(Integer comicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from SavedComic sc
            where sc.comic.id = :comicId
            """)
    void deleteAllByComicId(@Param("comicId") Integer comicId);
}
