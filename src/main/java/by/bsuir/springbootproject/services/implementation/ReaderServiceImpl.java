package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.ReaderData;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ComicPageRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.ReaderService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReaderServiceImpl implements ReaderService {

    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public ReaderData getReaderData(Integer translationId) {
        Translation translation = translationRepository.findReaderTranslationById(translationId)
                .orElseThrow(() -> new RuntimeException("Перевод не найден или не одобрен: " + translationId));

        List<ComicPage> pages = comicPageRepository.findByTranslationIdOrderByPageNumberAsc(translationId);

        if (pages.isEmpty()) {
            throw new RuntimeException("Страницы перевода отсутствуют: " + translationId);
        }

        Integer comicId = translation.getChapter().getComic().getId();
        Integer chapterNumber = translation.getChapter().getChapterNumber();
        Integer languageId = translation.getLanguage().getId();

        Translation prev = translationRepository.findPrevApprovedSameLanguage(
                        comicId, chapterNumber, languageId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseGet(() -> translationRepository.findPrevApprovedAnyLanguage(
                                comicId, chapterNumber, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .orElse(null));

        Translation next = translationRepository.findNextApprovedSameLanguage(
                        comicId, chapterNumber, languageId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseGet(() -> translationRepository.findNextApprovedAnyLanguage(
                                comicId, chapterNumber, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .orElse(null));

        return new ReaderData(
                translation.getChapter().getComic(),
                translation,
                pages,
                prev,
                next
        );
    }

    @Override
    @Transactional
    public void markChapterReadIfAuthenticated(Integer chapterId) {
        SecurityContextUtils.getUser().ifPresent(user -> jdbcTemplate.update("""
                insert into read_chapters(user_id, chapter_id)
                values (?, ?)
                on conflict do nothing
                """, user.getId(), chapterId));
    }

    @Override
    public Integer getSavedPageIfAuthenticated(Integer translationId) {
        return SecurityContextUtils.getUser().map(user -> {
            try {
                Integer page = jdbcTemplate.queryForObject("""
                        select current_page
                        from read_progress
                        where user_id = ? and translation_id = ?
                        """, Integer.class, user.getId(), translationId);
                return page != null && page > 0 ? page : 1;
            } catch (EmptyResultDataAccessException e) {
                return 1;
            }
        }).orElse(1);
    }

    @Override
    @Transactional
    public void saveProgressIfAuthenticated(Integer translationId, Integer page) {
        if (page == null || page < 1) {
            return;
        }

        SecurityContextUtils.getUser().ifPresent(user -> jdbcTemplate.update("""
                insert into read_progress(user_id, translation_id, current_page, updated_at)
                values (?, ?, ?, now())
                on conflict (user_id, translation_id)
                do update set current_page = excluded.current_page,
                              updated_at = now()
                """, user.getId(), translationId, page));
    }
}