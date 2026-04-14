package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query("""
            select c
            from Chapter c
            where c.comic.id = :comicId
              and (:q = '' or str(c.chapterNumber) like concat(:q, '%'))
            """)
    Page<Chapter> searchByComicIdAndQuery(@Param("comicId") Integer comicId,
                                          @Param("q") String q,
                                          Pageable pageable);
}
