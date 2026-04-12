package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Translation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Integer> {

    @Query("""
       select t.chapter.id, t.language.name
       from Translation t
       join t.reviewStatus rs
       where t.chapter.id in :ids and rs.name = 'Одобрено'
       group by t.chapter.id, t.language.name
       """)
    List<Object[]> findApprovedLangsByChapterIds(@Param("ids") List<Integer> chapterIds);

    @Query(
            value = """
        select t
        from Translation t
        join fetch t.language
        join fetch t.translationType
        left join fetch t.user
        join t.reviewStatus rs
        where t.chapter.id = :chapterId
          and rs.name = 'Одобрено'
        """,
            countQuery = """
        select count(t)
        from Translation t
        join t.reviewStatus rs
        where t.chapter.id = :chapterId
          and rs.name = 'Одобрено'
        """
    )
    Page<Translation> findApprovedByChapterId(@Param("chapterId") Integer chapterId, Pageable pageable);

    @Query(
            value = """
        select t
        from Translation t
        join fetch t.language l
        join fetch t.translationType
        left join fetch t.user
        join t.reviewStatus rs
        where t.chapter.id = :chapterId
          and rs.name = 'Одобрено'
          and lower(l.name) = lower(:lang)
        """,
            countQuery = """
        select count(t)
        from Translation t
        join t.reviewStatus rs
        join t.language l
        where t.chapter.id = :chapterId
          and rs.name = 'Одобрено'
          and lower(l.name) = lower(:lang)
        """
    )
    Page<Translation> findApprovedByChapterIdAndLang(@Param("chapterId") Integer chapterId,
                                                     @Param("lang") String lang,
                                                     Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where rs.name = 'Одобрено'
        order by t.createdAt desc
        """)
    List<Translation> findRecentApprovedTranslations(Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join fetch t.translationType tt
        left join fetch t.user u
        join t.reviewStatus rs
        where t.id = :id
          and rs.name = 'Одобрено'
        """)
    Optional<Translation> findReaderTranslationById(@Param("id") Integer id);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and l.id = :languageId
          and rs.name = 'Одобрено'
          and ch.chapterNumber < :chapterNumber
        order by ch.chapterNumber desc, t.createdAt desc
        """)
    List<Translation> findPrevApprovedSameLanguage(@Param("comicId") Integer comicId,
                                                   @Param("chapterNumber") Integer chapterNumber,
                                                   @Param("languageId") Integer languageId,
                                                   Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and l.id = :languageId
          and rs.name = 'Одобрено'
          and ch.chapterNumber > :chapterNumber
        order by ch.chapterNumber asc, t.createdAt desc
        """)
    List<Translation> findNextApprovedSameLanguage(@Param("comicId") Integer comicId,
                                                   @Param("chapterNumber") Integer chapterNumber,
                                                   @Param("languageId") Integer languageId,
                                                   Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and rs.name = 'Одобрено'
          and ch.chapterNumber < :chapterNumber
        order by ch.chapterNumber desc, t.createdAt desc
        """)
    List<Translation> findPrevApprovedAnyLanguage(@Param("comicId") Integer comicId,
                                                  @Param("chapterNumber") Integer chapterNumber,
                                                  Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and rs.name = 'Одобрено'
          and ch.chapterNumber > :chapterNumber
        order by ch.chapterNumber asc, t.createdAt desc
        """)
    List<Translation> findNextApprovedAnyLanguage(@Param("comicId") Integer comicId,
                                                  @Param("chapterNumber") Integer chapterNumber,
                                                  Pageable pageable);
}