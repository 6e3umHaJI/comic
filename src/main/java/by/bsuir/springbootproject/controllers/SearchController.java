package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public String openSearch(@RequestParam(defaultValue = "") String q, RedirectAttributes redirectAttributes) {
        String query = q == null ? "" : q.trim();

        if (StringUtils.hasText(query)) {
            redirectAttributes.addAttribute("filter", "search");
            redirectAttributes.addAttribute("value", query);
            return "redirect:/catalog/apply";
        }

        return "redirect:/catalog";
    }

    @GetMapping("/quick")
    @ResponseBody
    public Map<String, Object> quickSearch(@RequestParam(defaultValue = "") String q,
                                           @RequestParam(defaultValue = "0") int page) {
        return searchService.quickSearch(q, page);
    }
}
