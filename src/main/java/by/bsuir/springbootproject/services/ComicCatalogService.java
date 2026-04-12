package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.SearchCriteria;
import org.springframework.web.servlet.ModelAndView;

public interface ComicCatalogService {
    ModelAndView findComics(SearchCriteria criteria);
}