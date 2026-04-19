package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.AdminComplaintService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private final AdminComplaintService adminComplaintService;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping
    public ModelAndView page(@RequestParam(defaultValue = "TRANSLATION") String scope,
                             @RequestParam(defaultValue = "") String typeId,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "desc") String sortDirection,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        requireAdmin();

        ModelAndView mv = adminComplaintService.getComplaintsPage(scope, typeId, q, sortDirection, page);
        mv.setViewName(XML_HTTP_REQUEST.equals(requestedWith)
                ? "admin/complaints-content"
                : "admin/complaints-page");
        return mv;
    }

    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestParam Integer complaintId,
                                                            @RequestParam Integer statusId) {
        requireAdmin();

        try {
            String statusName = adminComplaintService.updateComplaintStatus(complaintId, statusId);
            boolean removed = "Решена".equals(statusName) || "Отклонена".equals(statusName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "complaintId", complaintId,
                    "statusId", statusId,
                    "statusName", statusName,
                    "removed", removed
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
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
