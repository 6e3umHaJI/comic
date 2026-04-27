package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.TranslationSubmissionService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TranslationSubmissionController {

    private final TranslationSubmissionService translationSubmissionService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping("/comics/{comicId}/chapters/new")
    public Object createPage(@PathVariable Integer comicId,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (user == null) {
            if (ajax) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AUTH_REQUIRED");
            }
            return new ModelAndView("redirect:/auth/login");
        }

        try {
            return translationSubmissionService.getCreatePage(comicId, user, null, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (ajax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }

            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
            return new ModelAndView("redirect:/comics/" + comicId + "?tab=chapters");
        }
    }

    @PostMapping("/comics/{comicId}/chapters/new")
    public ModelAndView submit(@PathVariable Integer comicId,
                               @ModelAttribute("form") TranslationSubmissionForm form,
                               @RequestParam(value = "pageFiles", required = false) MultipartFile[] pageFiles,
                               @RequestParam(value = "selectedPageNumbers", required = false) List<Integer> selectedPageNumbers,
                               RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/auth/login");
        }

        form.setSelectedPageNumbers(selectedPageNumbers);

        try {
            Integer translationId = translationSubmissionService.submit(comicId, user, form, pageFiles);
            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод успешно сохранён.");
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
    public Object approve(@PathVariable Integer translationId,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (!isAdmin(user)) {
            if (ajax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Недостаточно прав."
                        ));
            }

            redirectAttributes.addFlashAttribute("uploadError", "Недостаточно прав.");
            return new ModelAndView("redirect:/home");
        }

        try {
            translationSubmissionService.approve(translationId, user);

            if (ajax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Перевод подтверждён.",
                        "statusName", "Одобрено",
                        "readerUrl", request.getContextPath() + "/read/" + translationId
                ));
            }

            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод подтверждён.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (ajax) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }

            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        }

        return new ModelAndView("redirect:/translations/" + translationId + "/preview");
    }

    @PostMapping("/admin/translations/{translationId}/reject")
    public Object reject(@PathVariable Integer translationId,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         @RequestParam(required = false, defaultValue = "false") boolean revokeRights,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        User user = securityContextUtils.getUserFromContext().orElse(null);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (!isAdmin(user)) {
            if (ajax) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Недостаточно прав."
                        ));
            }

            redirectAttributes.addFlashAttribute("uploadError", "Недостаточно прав.");
            return new ModelAndView("redirect:/home");
        }

        try {
            translationSubmissionService.reject(translationId, user, reason, revokeRights);

            if (ajax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Перевод отклонён и удалён.",
                        "redirectUrl", request.getContextPath() + "/admin/translations/review"
                ));
            }

            redirectAttributes.addFlashAttribute("uploadMessage", "Перевод отклонён и удалён.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (ajax) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }

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