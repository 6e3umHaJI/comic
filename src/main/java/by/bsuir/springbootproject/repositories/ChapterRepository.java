package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query("""
            select c
            from Chapter c
            where c.comic.id = :comicId
              and (:q = '' or str(c.chapterNumber) like concat(:q, '%'))
              and exists (
                    select 1
                    from Translation t
                    join t.reviewStatus rs
                    where t.chapter = c
                      and rs.name = 'Одобрено'
              )
            """)
    Page<Chapter> searchByComicIdAndQuery(@Param("comicId") Integer comicId,
                                          @Param("q") String q,
                                          Pageable pageable);

    @Query("""
        select max(c.chapterNumber)
        from Chapter c
        where c.comic.id = :comicId
          and c.chapterNumber < :chapterNumber
        """)
    Optional<Integer> findPrevChapterNumber(@Param("comicId") Integer comicId,
                                            @Param("chapterNumber") Integer chapterNumber);

    @Query("""
        select min(c.chapterNumber)
        from Chapter c
        where c.comic.id = :comicId
          and c.chapterNumber > :chapterNumber
        """)
    Optional<Integer> findNextChapterNumber(@Param("comicId") Integer comicId,
                                            @Param("chapterNumber") Integer chapterNumber);

    Optional<Chapter> findByComic_IdAndChapterNumber(Integer comicId, Integer chapterNumber);

    long countByComic_Id(Integer comicId);
}
