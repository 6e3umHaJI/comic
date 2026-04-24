package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Translation;
import org.springframework.data.domain.Page;
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
    join fetch t.language l
    join t.reviewStatus rs
    where ch.comic.id = :comicId
      and rs.name = 'Одобрено'
    order by ch.chapterNumber asc, t.createdAt asc, t.id asc
    """)
    List<Translation> findFirstApprovedByComic(@Param("comicId") Integer comicId, Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and ch.chapterNumber = :chapterNumber
          and l.id = :languageId
          and rs.name = 'Одобрено'
        order by t.createdAt desc, t.id desc
        """)
    List<Translation> findApprovedSameLanguageByComicAndChapterNumber(@Param("comicId") Integer comicId,
                                                                      @Param("chapterNumber") Integer chapterNumber,
                                                                      @Param("languageId") Integer languageId);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join t.reviewStatus rs
        where c.id = :comicId
          and ch.chapterNumber = :chapterNumber
          and rs.name = 'Одобрено'
        order by t.createdAt desc, t.id desc
        """)
    List<Translation> findApprovedAnyLanguageByComicAndChapterNumber(@Param("comicId") Integer comicId,
                                                                     @Param("chapterNumber") Integer chapterNumber);
    long countByUser_Id(Integer userId);

    long countByUser_IdAndReviewStatus_Name(Integer userId, String reviewStatusName);

    @Query("""
    select distinct ch.chapterNumber
    from Translation t
    join t.chapter ch
    join t.reviewStatus rs
    where ch.comic.id = :comicId
      and t.language.id = :languageId
      and rs.name = 'Одобрено'
    order by ch.chapterNumber asc
    """)
    List<Integer> findDistinctApprovedChapterNumbersByComicAndLanguage(@Param("comicId") Integer comicId,
                                                                       @Param("languageId") Integer languageId);


    @Query("""
    select count(t)
    from Translation t
    join t.reviewStatus rs
    where t.chapter.id = :chapterId
      and rs.name = 'Одобрено'
    """)
    long countApprovedByChapterId(@Param("chapterId") Integer chapterId);

    long countByChapter_Id(Integer chapterId);

    @EntityGraph(attributePaths = {"chapter", "chapter.comic", "language", "translationType", "reviewStatus", "user"})
    List<Translation> findAllByChapter_Comic_IdOrderByChapter_ChapterNumberAscIdAsc(Integer comicId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Translation t
        where t.chapter.comic.id = :comicId
        """)
    void deleteAllByComicId(@Param("comicId") Integer comicId);


    @Query("""
    select distinct t
    from Translation t
    join fetch t.chapter ch
    join fetch ch.comic c
    join fetch t.language l
    join fetch t.translationType tt
    join fetch t.reviewStatus rs
    left join fetch t.user u
    where t.id = :id
    """)
    Optional<Translation> findSubmissionPreviewById(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"chapter", "chapter.comic", "language", "translationType", "reviewStatus", "user"})
    @Query(
            value = """
            select t
            from Translation t
            join t.reviewStatus rs
            join t.chapter ch
            join ch.comic c
            left join t.user u
            where rs.name = :statusName
              and (
                    :q = ''
                    or lower(c.title) like lower(concat('%', :q, '%'))
                    or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                    or lower(t.title) like lower(concat('%', :q, '%'))
                    or lower(u.username) like lower(concat('%', :q, '%'))
              )
            """,
            countQuery = """
            select count(t)
            from Translation t
            join t.reviewStatus rs
            join t.chapter ch
            join ch.comic c
            left join t.user u
            where rs.name = :statusName
              and (
                    :q = ''
                    or lower(c.title) like lower(concat('%', :q, '%'))
                    or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                    or lower(t.title) like lower(concat('%', :q, '%'))
                    or lower(u.username) like lower(concat('%', :q, '%'))
              )
            """
    )
    Page<Translation> findModerationPageByStatusNameAndQuery(@Param("statusName") String statusName,
                                                             @Param("q") String q,
                                                             Pageable pageable);


    @EntityGraph(attributePaths = {"chapter", "chapter.comic", "language", "translationType", "reviewStatus"})
    @Query(
            value = """
                select t
                from Translation t
                join t.chapter ch
                join ch.comic c
                where t.user.id = :userId
                  and (
                      :q = ''
                      or lower(c.title) like lower(concat('%', :q, '%'))
                      or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                  )
                """,
            countQuery = """
                select count(t)
                from Translation t
                join t.chapter ch
                join ch.comic c
                where t.user.id = :userId
                  and (
                      :q = ''
                      or lower(c.title) like lower(concat('%', :q, '%'))
                      or lower(c.originalTitle) like lower(concat('%', :q, '%'))
                  )
                """
    )
    Page<Translation> findUserUploadedPage(@Param("userId") Integer userId,
                                           @Param("q") String q,
                                           Pageable pageable);

    @Query("""
        select t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join fetch t.translationType tt
        left join fetch t.user u
        left join fetch t.reviewStatus rs
        where t.id = :id
        """)
    Optional<Translation> findReaderPreviewById(@Param("id") Integer id);

    @Query("""
        select max(ch.chapterNumber)
        from Translation t
        join t.chapter ch
        where ch.comic.id = :comicId
          and t.language.id = :languageId
        """)
    Integer findMaxChapterNumberByComicIdAndLanguageId(@Param("comicId") Integer comicId,
                                                       @Param("languageId") Integer languageId);

    @Query("""
        select distinct t
        from Translation t
        join fetch t.chapter ch
        join fetch ch.comic c
        join fetch t.language l
        join fetch t.translationType tt
        join fetch t.reviewStatus rs
        left join fetch t.user u
        where t.id = :id
        """)
    Optional<Translation> findAdminManageById(@Param("id") Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Translation t
        where t.id = :translationId
        """)
    void deleteHardById(@Param("translationId") Integer translationId);
}