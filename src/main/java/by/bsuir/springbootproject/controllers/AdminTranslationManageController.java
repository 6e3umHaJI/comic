package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.AdminTranslationEditForm;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.AdminTranslationManageService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminTranslationManageController {

    private final AdminTranslationManageService adminTranslationManageService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping("/admin/translations/{translationId}/edit")
    public ModelAndView editPage(@PathVariable Integer translationId,
                                 RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);

        try {
            return adminTranslationManageService.getEditPage(translationId, user, null, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
            return new ModelAndView("redirect:/home");
        }
    }

    @PostMapping("/admin/translations/{translationId}/save")
    public ModelAndView save(@PathVariable Integer translationId,
                             @ModelAttribute("form") AdminTranslationEditForm form,
                             @RequestParam("pagesPayload") String pagesPayload,
                             MultipartHttpServletRequest multipartRequest,
                             RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);

        try {
            adminTranslationManageService.updateTranslation(translationId, user, form, pagesPayload, multipartRequest);
            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод обновлён.");
            return new ModelAndView("redirect:/admin/translations/" + translationId + "/edit");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return adminTranslationManageService.getEditPage(translationId, user, form, e.getMessage());
        }
    }

    @PostMapping("/admin/translations/{translationId}/delete")
    public ModelAndView delete(@PathVariable Integer translationId,
                               RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);

        try {
            Integer comicId = adminTranslationManageService.deleteTranslation(translationId, user);
            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод удалён.");
            return new ModelAndView("redirect:/comics/" + comicId + "?tab=chapters");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return adminTranslationManageService.getEditPage(translationId, user, null, e.getMessage());
        }
    }
}
