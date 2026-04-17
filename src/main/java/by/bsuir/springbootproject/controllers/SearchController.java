package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.QuickSearchComicItem;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.repositories.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int QUICK_SEARCH_PAGE_SIZE = 20;

    private final ComicRepository comicRepository;

    @GetMapping
    public String openSearch(@RequestParam(defaultValue = "") String q,
                             RedirectAttributes redirectAttributes) {
        String query = q == null ? "" : q.trim();

        if (StringUtils.hasText(query)) {
            redirectAttributes.addAttribute("filter", "search");
            redirectAttributes.addAttribute("value", query);
            return "redirect:" + "/catalog" + "/apply";
        }

        return "redirect:" + "/catalog";
    }

    @GetMapping("/quick")
    @ResponseBody
    public Map<String, Object> quickSearch(@RequestParam(defaultValue = "") String q,
                                           @RequestParam(defaultValue = "0") int page) {
        String query = q == null ? "" : q.trim();
        int safePage = Math.max(page, 0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("page", safePage);
        response.put("pageSize", QUICK_SEARCH_PAGE_SIZE);

        if (!StringUtils.hasText(query)) {
            response.put("total", 0L);
            response.put("items", List.of());
            return response;
        }

        Pageable pageable = PageRequest.of(safePage, QUICK_SEARCH_PAGE_SIZE);
        Page<Comic> result = comicRepository.findQuickSearchResults(query, pageable);

        List<QuickSearchComicItem> items = result.getContent().stream()
                .map(comic -> new QuickSearchComicItem(
                        comic.getId(),
                        comic.getTitle(),
                        comic.getOriginalTitle(),
                        comic.getShortDescription(),
                        comic.getCover()
                ))
                .toList();

        response.put("total", result.getTotalElements());
        response.put("items", items);

        return response;
    }
}
