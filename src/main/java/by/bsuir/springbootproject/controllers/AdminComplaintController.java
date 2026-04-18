package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.AdminComplaintService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final AdminComplaintService adminComplaintService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping
    public ModelAndView page(@RequestParam(defaultValue = "TRANSLATION") String scope,
                             @RequestParam(defaultValue = "") String typeId,
                             @RequestParam(defaultValue = "desc") String sortDirection,
                             @RequestParam(defaultValue = "0") int page) {
        User user = requireAdmin();
        return adminComplaintService.getComplaintsPage(scope, typeId, sortDirection, page);
    }

    @PostMapping("/status")
    public String updateStatus(@RequestParam Integer complaintId,
                               @RequestParam Integer statusId,
                               @RequestParam(defaultValue = "TRANSLATION") String scope,
                               @RequestParam(defaultValue = "") String typeId,
                               @RequestParam(defaultValue = "desc") String sortDirection,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes redirectAttributes) {
        User user = requireAdmin();

        adminComplaintService.updateComplaintStatus(complaintId, statusId);

        redirectAttributes.addAttribute("scope", scope);
        if (typeId != null && !typeId.isBlank()) {
            redirectAttributes.addAttribute("typeId", typeId);
        }
        redirectAttributes.addAttribute("sortDirection", sortDirection);
        redirectAttributes.addAttribute("page", page);

        return "redirect:/admin/complaints";
    }

    private User requireAdmin() {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        if (user.getRole() == null || user.getRole().getName() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Недостаточно прав.");
        }

        return user;
    }
}
