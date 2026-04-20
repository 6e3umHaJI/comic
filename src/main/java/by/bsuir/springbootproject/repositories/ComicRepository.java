package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Comic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComicRepository extends JpaRepository<Comic, Integer>, JpaSpecificationExecutor<Comic> {

    List<Comic> findTop10ByOrderByPopularityScoreDesc();

    List<Comic> findTop10ByOrderByUpdatedAtDesc();

    List<Comic> findTop10ByOrderByCreatedAtDesc();

    @Query("""
            SELECT cr.relatedComic
            FROM ComicRelation cr
            WHERE cr.comic.id = :id
            """)
    List<Comic> findRelatedComics(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT c
            FROM Comic c
            JOIN c.genres g
            WHERE g IN (
                SELECT g2
                FROM Comic c2
                JOIN c2.genres g2
                WHERE c2.id = :id
            )
            AND c.id <> :id
            ORDER BY c.popularityScore DESC
            """)
    List<Comic> findSimilarComics(@Param("id") Integer id);

    @Query("""
            SELECT rs.value, COUNT(r.id)
            FROM Rating r
            JOIN r.score rs
            WHERE r.comic.id = :comicId
            GROUP BY rs.value
            ORDER BY rs.value DESC
            """)
    List<Object[]> getRatingDistribution(@Param("comicId") Integer comicId);

    @Query("""
            SELECT CASE WHEN us.isDefault = true THEN us.name ELSE 'Другие' END,
                   COUNT(DISTINCT us.user.id)
            FROM SavedComic sc
            JOIN sc.section us
            WHERE sc.comic.id = :comicId
            GROUP BY CASE WHEN us.isDefault = true THEN us.name ELSE 'Другие' END
            """)
    List<Object[]> getFavoriteStats(@Param("comicId") Integer comicId);

    @Query("""
            SELECT l.name, COUNT(tr.id)
            FROM Translation tr
            JOIN tr.language l
            WHERE tr.chapter.comic.id = :comicId
              AND tr.reviewStatus.name = 'Одобрено'
            GROUP BY l.name
            ORDER BY COUNT(tr.id) DESC
            """)
    List<Object[]> getApprovedLangStatsByComic(@Param("comicId") int comicId);

    @Query("""
            SELECT ch.id, l.name
            FROM Translation tr
            JOIN tr.language l
            JOIN tr.chapter ch
            WHERE ch.comic.id = :comicId
              AND tr.reviewStatus.name = 'Одобрено'
            """)
    List<Object[]> getApprovedLangPairsByComic(@Param("comicId") int comicId);

    @Query("""
            SELECT COUNT(DISTINCT cr.relatedComic.id)
            FROM ComicRelation cr
            WHERE cr.comic.id = :id
            """)
    long countRelatedByComic(@Param("id") int id);

    @Query("""
            SELECT cr.relatedComic, rt.name
            FROM ComicRelation cr
            LEFT JOIN cr.relationType rt
            WHERE cr.comic.id = :id
            ORDER BY cr.relatedComic.popularityScore DESC,
                     cr.relatedComic.id DESC,
                     rt.name ASC
            """)
    List<Object[]> findRelatedComicsWithTypes(@Param("id") int id);

    @Query("""
            SELECT DISTINCT c
            FROM Comic c
            LEFT JOIN FETCH c.type
            LEFT JOIN FETCH c.ageRating
            LEFT JOIN FETCH c.comicStatus
            LEFT JOIN FETCH c.genres
            LEFT JOIN FETCH c.tags
            WHERE c.id = :id
            """)
    Optional<Comic> findByIdForComicPage(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT c
            FROM Comic c
            LEFT JOIN FETCH c.type
            LEFT JOIN FETCH c.ageRating
            LEFT JOIN FETCH c.comicStatus
            LEFT JOIN FETCH c.genres
            LEFT JOIN FETCH c.tags
            LEFT JOIN FETCH c.relatedComics rc
            LEFT JOIN FETCH rc.relatedComic
            LEFT JOIN FETCH rc.relationType
            WHERE c.id = :id
            """)
    Optional<Comic> findByIdForAdminEdit(@Param("id") Integer id);

    @Override
    @EntityGraph(attributePaths = {"genres"})
    Page<Comic> findAll(Specification<Comic> spec, Pageable pageable);

    @Query(
            value = """
                    SELECT c
                    FROM Comic c
                    WHERE lower(c.title) LIKE lower(concat('%', :q, '%'))
                       OR lower(c.originalTitle) LIKE lower(concat('%', :q, '%'))
                    ORDER BY c.popularityScore DESC, c.avgRating DESC, c.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM Comic c
                    WHERE lower(c.title) LIKE lower(concat('%', :q, '%'))
                       OR lower(c.originalTitle) LIKE lower(concat('%', :q, '%'))
                    """
    )
    Page<Comic> findQuickSearchResults(@Param("q") String q, Pageable pageable);
}
