package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.SessionAttributesNames;
import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.constants.ViewPaths;
import by.bsuir.springbootproject.entities.SearchCriteria;
import by.bsuir.springbootproject.services.ComicCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(RoutePaths.CATALOG)
@SessionAttributes(SessionAttributesNames.SEARCH_CRITERIA)
@RequiredArgsConstructor
public class ComicCatalogController {

    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final String PRESET_PATH = "/preset";
    private static final String MODE_POPULAR = "popular";
    private static final String MODE_NEW = "new";
    private static final String APPLY_PATH = "/apply";

    private static final String FILTER_TYPE = "type";
    private static final String FILTER_AGE = "age";
    private static final String FILTER_STATUS = "status";
    private static final String FILTER_LANGUAGE = "language";
    private static final String FILTER_GENRE = "genre";
    private static final String FILTER_TAG = "tag";
    private static final String FILTER_YEAR = "year";


    private final ComicCatalogService catalogService;

    @GetMapping
    public ModelAndView openCatalog(
            @ModelAttribute(SessionAttributesNames.SEARCH_CRITERIA) SearchCriteria criteria,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith
    ) {
        ModelAndView mv = catalogService.findComics(criteria);

        if (XML_HTTP_REQUEST.equals(requestedWith)) {
            mv.setViewName(ViewPaths.CATALOG_CONTENT);
        } else {
            mv.setViewName(ViewPaths.CATALOG);
        }

        return mv;
    }

    @PostMapping
    public ModelAndView postCatalog(
            @ModelAttribute(SessionAttributesNames.SEARCH_CRITERIA) SearchCriteria criteria,
            @RequestParam(required = false) String reset,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith
    ) {
        if ("true".equals(reset)) {
            criteria.reset();
        }

        ModelAndView mv = catalogService.findComics(criteria);
        mv.setViewName(XML_HTTP_REQUEST.equals(requestedWith)
                ? ViewPaths.CATALOG_CONTENT
                : ViewPaths.CATALOG);

        return mv;
    }

    @GetMapping(PRESET_PATH)
    public String applyHomePreset(
            @RequestParam String mode,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes
    ) {
        sessionStatus.setComplete();

        redirectAttributes.addAttribute("pageNumber", Values.DEFAULT_START_PAGE);
        redirectAttributes.addAttribute("viewMode", "card");

        if (MODE_NEW.equalsIgnoreCase(mode)) {
            redirectAttributes.addAttribute("sortField", "createdAt");
            redirectAttributes.addAttribute("sortDirection", "desc");
        } else {
            redirectAttributes.addAttribute("sortField", "popularityScore");
            redirectAttributes.addAttribute("sortDirection", "desc");
        }

        return "redirect:" + RoutePaths.CATALOG;
    }

    @GetMapping(APPLY_PATH)
    public String applySingleFilter(
            @RequestParam String filter,
            @RequestParam String value,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes
    ) {
        sessionStatus.setComplete();

        switch (filter) {
            case FILTER_TYPE -> redirectAttributes.addAttribute("selectedTypes", value);
            case FILTER_AGE -> redirectAttributes.addAttribute("selectedAgeRatings", value);
            case FILTER_STATUS -> redirectAttributes.addAttribute("selectedComicStatuses", value);
            case FILTER_LANGUAGE -> redirectAttributes.addAttribute("selectedLanguages", value);
            case FILTER_GENRE -> redirectAttributes.addAttribute("selectedGenres", value);
            case FILTER_TAG -> redirectAttributes.addAttribute("selectedTags", value);
            case FILTER_YEAR -> {
                redirectAttributes.addAttribute("releaseYearFrom", value);
                redirectAttributes.addAttribute("releaseYearTo", value);
            }
            default -> {
            }
        }

        return "redirect:" + RoutePaths.CATALOG;
    }

    @ModelAttribute(SessionAttributesNames.SEARCH_CRITERIA)
    public SearchCriteria initCriteria() {
        return SearchCriteria.builder()
                .pageNumber(Values.DEFAULT_START_PAGE)
                .pageSize(Values.DEFAULT_PAGE_SIZE)
                .keyWords("")
                .sortField("popularityScore")
                .sortDirection("desc")
                .viewMode("card")
                .build();
    }
}
