package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.TranslationSubmissionService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TranslationSubmissionController {

    private final TranslationSubmissionService translationSubmissionService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping("/comics/{comicId}/chapters/new")
    public ModelAndView createPage(@PathVariable Integer comicId,
                                   RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/auth/login");
        }

        try {
            return translationSubmissionService.getCreatePage(comicId, user, null, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
            return new ModelAndView("redirect:/comics/" + comicId + "?tab=chapters");
        }
    }

    @PostMapping("/comics/{comicId}/chapters/new")
    public ModelAndView submit(@PathVariable Integer comicId,
                               @ModelAttribute("form") TranslationSubmissionForm form,
                               @RequestParam(value = "pageFiles", required = false) MultipartFile[] pageFiles,
                               RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/auth/login");
        }

        try {
            Integer translationId = translationSubmissionService.submit(comicId, user, form, pageFiles);
            redirectAttributes.addFlashAttribute("uploadMessage", "Глава успешно сохранена.");
            return new ModelAndView("redirect:/translations/" + translationId + "/preview");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return translationSubmissionService.getCreatePage(comicId, user, form, e.getMessage());
        }
    }

    @GetMapping("/comics/{comicId}/chapters/options")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChapterOptions(@PathVariable Integer comicId,
                                                                 @RequestParam Integer languageId) {
        return ResponseEntity.ok(translationSubmissionService.getChapterOptions(comicId, languageId));
    }

    @GetMapping("/translations/{translationId}/preview")
    public ModelAndView preview(@PathVariable Integer translationId,
                                RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/auth/login");
        }

        try {
            return translationSubmissionService.getPreviewPage(translationId, user, null, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
            return new ModelAndView("redirect:/home");
        }
    }

    @GetMapping("/admin/translations/review")
    public ModelAndView moderationPage(@RequestParam(defaultValue = "") String q,
                                       @RequestParam(defaultValue = "desc") String sortDirection,
                                       @RequestParam(defaultValue = "0") int page,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (!isAdmin(user)) {
            redirectAttributes.addFlashAttribute("uploadError", "Недостаточно прав.");
            return new ModelAndView("redirect:/home");
        }

        ModelAndView mv = translationSubmissionService.getModerationPage(q, sortDirection, page);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        mv.setViewName(ajax ? "admin/translation-review-content" : "admin/translation-review-list");
        return mv;
    }

    @PostMapping("/admin/translations/{translationId}/approve")
    public ModelAndView approve(@PathVariable Integer translationId,
                                RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (!isAdmin(user)) {
            redirectAttributes.addFlashAttribute("uploadError", "Недостаточно прав.");
            return new ModelAndView("redirect:/home");
        }

        try {
            translationSubmissionService.approve(translationId, user);
            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод подтверждён.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        }
        return new ModelAndView("redirect:/translations/" + translationId + "/preview");
    }

    @PostMapping("/admin/translations/{translationId}/reject")
    public ModelAndView reject(@PathVariable Integer translationId,
                               @RequestParam(required = false, defaultValue = "") String reason,
                               @RequestParam(required = false, defaultValue = "false") boolean revokeRights,
                               RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (!isAdmin(user)) {
            redirectAttributes.addFlashAttribute("uploadError", "Недостаточно прав.");
            return new ModelAndView("redirect:/home");
        }

        try {
            translationSubmissionService.reject(translationId, user, reason, revokeRights);
            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод отклонён и удалён.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        }
        return new ModelAndView("redirect:/admin/translations/review");
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }
}