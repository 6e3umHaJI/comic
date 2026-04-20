package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.AgeRating;
import by.bsuir.springbootproject.entities.ComicStatus;
import by.bsuir.springbootproject.entities.ComicType;
import by.bsuir.springbootproject.entities.Genre;
import by.bsuir.springbootproject.entities.Language;
import by.bsuir.springbootproject.entities.SearchCriteria;
import by.bsuir.springbootproject.entities.Tag;
import by.bsuir.springbootproject.repositories.AgeRatingRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComicSearchSpecification;
import by.bsuir.springbootproject.repositories.ComicStatusRepository;
import by.bsuir.springbootproject.repositories.ComicTypeRepository;
import by.bsuir.springbootproject.repositories.GenreRepository;
import by.bsuir.springbootproject.repositories.LanguageRepository;
import by.bsuir.springbootproject.repositories.TagRepository;
import by.bsuir.springbootproject.services.ComicCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComicCatalogServiceImpl implements ComicCatalogService {

    private final ComicRepository comicRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final ComicTypeRepository typeRepository;
    private final ComicStatusRepository statusRepository;
    private final AgeRatingRepository ageRatingRepository;
    private final LanguageRepository languageRepository;

    @Override
    public ModelAndView findComics(SearchCriteria criteria) {
        var genres = genreRepository.findAll().stream()
                .sorted(Comparator.comparing(Genre::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var tags = tagRepository.findAll().stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var types = typeRepository.findAll().stream()
                .sorted(Comparator.comparing(ComicType::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var statuses = statusRepository.findAll().stream()
                .sorted(Comparator.comparing(ComicStatus::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var ageRatings = ageRatingRepository.findAll().stream()
                .sorted(Comparator.comparing(AgeRating::getId))
                .toList();

        var languages = languageRepository.findAll().stream()
                .sorted(Comparator.comparing(Language::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        sanitizeCriteria(criteria, genres, tags, types, statuses, ageRatings, languages);

        Sort sort = buildSort(criteria);
        Pageable pageable = PageRequest.of(
                criteria.getPageNumber(),
                criteria.getPageSize(),
                sort
        );

        Page<?> comics = comicRepository.findAll(new ComicSearchSpecification(criteria), pageable);

        int totalPages = comics.getTotalPages();
        int currentPage = comics.getNumber() + 1;
        int visiblePages = 5;
        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(beginPage + visiblePages - 1, totalPages);

        if (endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        boolean showLeftDots = beginPage > 2;
        boolean showRightDots = endPage < totalPages - 1;

        ModelAndView mv = new ModelAndView("catalog");
        mv.addObject("comics", comics.getContent());
        mv.addObject("searchCriteria", criteria);
        mv.addObject("currentPage", currentPage);
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
        mv.addObject("showLeftDots", showLeftDots);
        mv.addObject("showRightDots", showRightDots);
        mv.addObject("genres", genres);
        mv.addObject("tags", tags);
        mv.addObject("types", types);
        mv.addObject("statuses", statuses);
        mv.addObject("ageRatings", ageRatings);
        mv.addObject("languages", languages);

        return mv;
    }

    private void sanitizeCriteria(SearchCriteria criteria,
                                  java.util.List<Genre> genres,
                                  java.util.List<Tag> tags,
                                  java.util.List<ComicType> types,
                                  java.util.List<ComicStatus> statuses,
                                  java.util.List<AgeRating> ageRatings,
                                  java.util.List<Language> languages) {
        criteria.setSelectedGenres(sanitizeValues(
                criteria.getSelectedGenres(),
                genres.stream().map(Genre::getName).collect(Collectors.toSet())
        ));

        criteria.setSelectedTags(sanitizeValues(
                criteria.getSelectedTags(),
                tags.stream().map(Tag::getName).collect(Collectors.toSet())
        ));

        criteria.setSelectedTypes(sanitizeValues(
                criteria.getSelectedTypes(),
                types.stream().map(ComicType::getName).collect(Collectors.toSet())
        ));

        criteria.setSelectedComicStatuses(sanitizeValues(
                criteria.getSelectedComicStatuses(),
                statuses.stream().map(ComicStatus::getName).collect(Collectors.toSet())
        ));

        criteria.setSelectedAgeRatings(sanitizeValues(
                criteria.getSelectedAgeRatings(),
                ageRatings.stream().map(AgeRating::getName).collect(Collectors.toSet())
        ));

        criteria.setSelectedLanguages(sanitizeValues(
                criteria.getSelectedLanguages(),
                languages.stream().map(Language::getName).collect(Collectors.toSet())
        ));
    }

    private String[] sanitizeValues(String[] selectedValues, Set<String> allowedValues) {
        if (selectedValues == null || selectedValues.length == 0) {
            return new String[0];
        }

        return Arrays.stream(selectedValues)
                .filter(value -> value != null && allowedValues.contains(value))
                .distinct()
                .toArray(String[]::new);
    }

    private Sort buildSort(SearchCriteria criteria) {
        Sort.Direction direction = Sort.Direction.fromString(criteria.getSortDirection());
        String field = criteria.getSortField();

        if ("popularityScore".equals(field)) {
            return Sort.by(
                    new Sort.Order(direction, "popularityScore"),
                    new Sort.Order(Sort.Direction.DESC, "id")
            );
        }

        if ("createdAt".equals(field)) {
            return Sort.by(
                    new Sort.Order(direction, "createdAt"),
                    new Sort.Order(Sort.Direction.DESC, "id")
            );
        }

        return Sort.by(direction, field);
    }
}
