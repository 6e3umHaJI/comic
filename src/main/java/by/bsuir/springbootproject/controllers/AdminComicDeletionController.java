package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.AdminComicDeletionService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminComicDeletionController {

    private final AdminComicDeletionService adminComicDeletionService;
    private final SecurityContextUtils securityContextUtils;

    @PostMapping("/admin/comics/{comicId}/delete")
    public ModelAndView deleteComic(@PathVariable Integer comicId,
                                    RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);

        try {
            adminComicDeletionService.deleteComic(comicId, user);
            redirectAttributes.addFlashAttribute("uploadMessage", "Комикс удалён.");
            return new ModelAndView("redirect:/home");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
            return new ModelAndView("redirect:/comics/" + comicId);
        }
    }
}