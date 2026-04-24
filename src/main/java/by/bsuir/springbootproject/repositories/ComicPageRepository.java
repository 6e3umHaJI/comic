package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.ComicPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComicPageRepository extends JpaRepository<ComicPage, Integer> {

    List<ComicPage> findByTranslationIdOrderByPageNumberAsc(Integer translationId);

    List<ComicPage> findByTranslation_Chapter_Comic_IdOrderByTranslation_IdAscPageNumberAsc(Integer comicId);

    long countByTranslation_Id(Integer translationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from ComicPage p
            where p.translation.id = :translationId
            """)
    void deleteAllByTranslationId(@Param("translationId") Integer translationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from ComicPage p
            where p.translation.chapter.comic.id = :comicId
            """)
    void deleteAllByComicId(@Param("comicId") Integer comicId);
}
