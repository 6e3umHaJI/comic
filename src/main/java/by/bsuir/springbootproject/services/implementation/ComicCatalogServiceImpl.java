package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.entities.SearchCriteria;
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

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComicCatalogServiceImpl implements ComicCatalogService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 60;
    private static final int VISIBLE_PAGES = 5;

    private static final String DEFAULT_SORT_FIELD = "popularityScore";
    private static final String DEFAULT_SORT_DIRECTION = "desc";
    private static final String DEFAULT_VIEW_MODE = "card";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "popularityScore",
            "createdAt",
            "title",
            "releaseYear",
            "avgRating",
            "ratingsCount",
            "chaptersCount",
            "updatedAt",
            "id"
    );

    private final ComicRepository comicRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final ComicTypeRepository typeRepository;
    private final ComicStatusRepository statusRepository;
    private final AgeRatingRepository ageRatingRepository;
    private final LanguageRepository languageRepository;

    @Override
    public ModelAndView findComics(SearchCriteria criteria) {
        normalizeCriteria(criteria);

        Sort sort = buildSort(criteria);
        Pageable pageable = PageRequest.of(
                criteria.getPageNumber(),
                criteria.getPageSize(),
                sort
        );

        Page comics = comicRepository.findAll(new ComicSearchSpecification(criteria), pageable);

        int totalPages = comics.getTotalPages();
        int currentPage = comics.getNumber() + 1;

        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(beginPage + VISIBLE_PAGES - 1, totalPages);

        if (endPage - beginPage < VISIBLE_PAGES - 1) {
            beginPage = Math.max(1, endPage - VISIBLE_PAGES + 1);
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
        mv.addObject("genres", genreRepository.findAll());
        mv.addObject("tags", tagRepository.findAll());
        mv.addObject("types", typeRepository.findAll());
        mv.addObject("statuses", statusRepository.findAll());
        mv.addObject("ageRatings", ageRatingRepository.findAll());
        mv.addObject("languages", languageRepository.findAll());
        return mv;
    }

    private void normalizeCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Параметры поиска не переданы.");
        }

        if (criteria.getPageNumber() < Values.DEFAULT_START_PAGE) {
            criteria.setPageNumber(Values.DEFAULT_START_PAGE);
        }

        if (criteria.getPageSize() < MIN_PAGE_SIZE || criteria.getPageSize() > MAX_PAGE_SIZE) {
            criteria.setPageSize(Values.DEFAULT_PAGE_SIZE);
        }

        if (criteria.getSortField() == null || !ALLOWED_SORT_FIELDS.contains(criteria.getSortField())) {
            criteria.setSortField(DEFAULT_SORT_FIELD);
        }

        if (!"asc".equalsIgnoreCase(criteria.getSortDirection())
                && !"desc".equalsIgnoreCase(criteria.getSortDirection())) {
            criteria.setSortDirection(DEFAULT_SORT_DIRECTION);
        }

        if (!"card".equalsIgnoreCase(criteria.getViewMode())
                && !"list".equalsIgnoreCase(criteria.getViewMode())) {
            criteria.setViewMode(DEFAULT_VIEW_MODE);
        }

        if (criteria.getKeyWords() == null) {
            criteria.setKeyWords("");
        }

        criteria.setSelectedTypes(nonNullArray(criteria.getSelectedTypes()));
        criteria.setSelectedLanguages(nonNullArray(criteria.getSelectedLanguages()));
        criteria.setSelectedComicStatuses(nonNullArray(criteria.getSelectedComicStatuses()));
        criteria.setSelectedAgeRatings(nonNullArray(criteria.getSelectedAgeRatings()));
        criteria.setSelectedGenres(nonNullArray(criteria.getSelectedGenres()));
        criteria.setSelectedTags(nonNullArray(criteria.getSelectedTags()));

        normalizeRanges(criteria);
    }

    private String[] nonNullArray(String[] value) {
        return value == null ? new String[0] : value;
    }

    private void normalizeRanges(SearchCriteria criteria) {
        if (criteria.getReleaseYearFrom() != null && criteria.getReleaseYearTo() != null
                && criteria.getReleaseYearFrom() > criteria.getReleaseYearTo()) {
            Integer tmp = criteria.getReleaseYearFrom();
            criteria.setReleaseYearFrom(criteria.getReleaseYearTo());
            criteria.setReleaseYearTo(tmp);
        }

        if (criteria.getRatingsCountFrom() != null && criteria.getRatingsCountFrom() < 0) {
            criteria.setRatingsCountFrom(0);
        }

        if (criteria.getRatingsCountTo() != null && criteria.getRatingsCountTo() < 0) {
            criteria.setRatingsCountTo(0);
        }

        if (criteria.getRatingsCountFrom() != null && criteria.getRatingsCountTo() != null
                && criteria.getRatingsCountFrom() > criteria.getRatingsCountTo()) {
            Integer tmp = criteria.getRatingsCountFrom();
            criteria.setRatingsCountFrom(criteria.getRatingsCountTo());
            criteria.setRatingsCountTo(tmp);
        }

        if (criteria.getAvgRatingFrom() != null) {
            criteria.setAvgRatingFrom(clamp(criteria.getAvgRatingFrom(), 0.0, 10.0));
        }

        if (criteria.getAvgRatingTo() != null) {
            criteria.setAvgRatingTo(clamp(criteria.getAvgRatingTo(), 0.0, 10.0));
        }

        if (criteria.getAvgRatingFrom() != null && criteria.getAvgRatingTo() != null
                && criteria.getAvgRatingFrom() > criteria.getAvgRatingTo()) {
            Double tmp = criteria.getAvgRatingFrom();
            criteria.setAvgRatingFrom(criteria.getAvgRatingTo());
            criteria.setAvgRatingTo(tmp);
        }

        if (criteria.getChaptersCountFrom() != null && criteria.getChaptersCountFrom() < 0) {
            criteria.setChaptersCountFrom(0);
        }

        if (criteria.getChaptersCountTo() != null && criteria.getChaptersCountTo() < 0) {
            criteria.setChaptersCountTo(0);
        }

        if (criteria.getChaptersCountFrom() != null && criteria.getChaptersCountTo() != null
                && criteria.getChaptersCountFrom() > criteria.getChaptersCountTo()) {
            Integer tmp = criteria.getChaptersCountFrom();
            criteria.setChaptersCountFrom(criteria.getChaptersCountTo());
            criteria.setChaptersCountTo(tmp);
        }

        if (criteria.getUpdatedFrom() != null && criteria.getUpdatedTo() != null
                && criteria.getUpdatedFrom().isAfter(criteria.getUpdatedTo())) {
            var tmp = criteria.getUpdatedFrom();
            criteria.setUpdatedFrom(criteria.getUpdatedTo());
            criteria.setUpdatedTo(tmp);
        }
    }

    private Double clamp(Double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Sort buildSort(SearchCriteria criteria) {
        Sort.Direction direction = "asc".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

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

        return Sort.by(
                new Sort.Order(direction, field),
                new Sort.Order(Sort.Direction.DESC, "id")
        );
    }
}
