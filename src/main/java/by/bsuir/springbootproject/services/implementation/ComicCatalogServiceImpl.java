package by.bsuir.springbootproject.services.implementation;

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
        Sort sort = Sort.by(
                Sort.Direction.fromString(criteria.getSortDirection()),
                criteria.getSortField()
        );

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
        mv.addObject("genres", genreRepository.findAll());
        mv.addObject("tags", tagRepository.findAll());
        mv.addObject("types", typeRepository.findAll());
        mv.addObject("statuses", statusRepository.findAll());
        mv.addObject("ageRatings", ageRatingRepository.findAll());
        mv.addObject("languages", languageRepository.findAll());

        return mv;
    }
}
