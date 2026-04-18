package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.ComplaintService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final SecurityContextUtils securityContextUtils;

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitComplaint(@RequestParam Integer targetId,
                                                               @RequestParam Integer complaintTypeId,
                                                               @RequestParam String description) {
        User user = securityContextUtils.getUserFromContext().orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Авторизуйтесь, чтобы отправить жалобу."
                    ));
        }

        try {
            complaintService.submitComplaint(
                    user.getId(),
                    targetId,
                    complaintTypeId,
                    description
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Жалоба успешно отправлена."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
