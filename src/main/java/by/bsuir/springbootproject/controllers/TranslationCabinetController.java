package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.TranslationCabinetService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile/translations")
public class TranslationCabinetController {

    private static final int PAGE_SIZE = 3;

    private final TranslationCabinetService translationCabinetService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping
    public String openPage(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "desc") String sortDirection,
                           @RequestParam(defaultValue = "0") Integer page,
                           HttpServletRequest request,
                           Model model) {

        Integer userId = securityContextUtils.getUserFromContext()
                .map(User::getId)
                .orElse(null);

        if (userId == null) {
            return "redirect:/auth/login";
        }

        Page<Translation> translationsPage = translationCabinetService.getUserTranslationsPage(
                userId,
                q,
                sortDirection,
                page == null ? 0 : Math.max(page, 0),
                PAGE_SIZE
        );

        fillModel(model, translationsPage, q, sortDirection);

        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        return ajax ? "translation/cabinet-content" : "translation/cabinet-page";
    }

    private void fillModel(Model model,
                           Page<Translation> translationsPage,
                           String q,
                           String sortDirection) {

        int totalPages = translationsPage.getTotalPages();
        int currentPage = totalPages == 0 ? 1 : translationsPage.getNumber() + 1;

        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(Math.max(totalPages, 1), currentPage + 2);

        if (endPage - beginPage < 4) {
            beginPage = Math.max(1, endPage - 4);
            endPage = Math.min(Math.max(totalPages, 1), beginPage + 4);
        }

        model.addAttribute("translations", translationsPage.getContent());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("sortDirection", "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc");
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("currentPageZeroBased", Math.max(translationsPage.getNumber(), 0));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("beginPage", beginPage);
        model.addAttribute("endPage", endPage);
    }
}
