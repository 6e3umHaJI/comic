package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.PasswordResetRequestForm;
import by.bsuir.springbootproject.dto.PasswordResetVerifyForm;
import by.bsuir.springbootproject.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reset")
public class PasswordResetController {

    private static final String STEP_REQUEST = "request";
    private static final String STEP_VERIFY = "verify";
    private static final String STATUS_COLOR_GREEN = "green";
    private static final String STATUS_COLOR_RED = "red";

    private final PasswordResetService passwordResetService;

    @GetMapping
    public String showRequestPage(Model model,
                                  @RequestParam(required = false) String login) {
        if (!model.containsAttribute("requestForm")) {
            PasswordResetRequestForm form = new PasswordResetRequestForm();
            form.setLogin(login);
            model.addAttribute("requestForm", form);
        }

        if (!model.containsAttribute("verifyForm")) {
            model.addAttribute("verifyForm", new PasswordResetVerifyForm());
        }

        model.addAttribute("step", STEP_REQUEST);
        return "auth/password-reset";
    }

    @PostMapping("/send-code")
    public String sendCode(@Valid @ModelAttribute("requestForm") PasswordResetRequestForm requestForm,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("verifyForm", new PasswordResetVerifyForm());
            model.addAttribute("step", STEP_REQUEST);
            return "auth/password-reset";
        }

        try {
            String maskedEmail = passwordResetService.sendCode(requestForm.getLogin());
            model.addAttribute("verifyForm", new PasswordResetVerifyForm());
            model.addAttribute("step", STEP_VERIFY);
            model.addAttribute("login", requestForm.getLogin());
            model.addAttribute("maskedEmail", maskedEmail);
            model.addAttribute("status", "Код отправлен на " + maskedEmail);
            model.addAttribute("statusColor", STATUS_COLOR_GREEN);
            return "auth/password-reset";
        } catch (RuntimeException e) {
            model.addAttribute("verifyForm", new PasswordResetVerifyForm());
            model.addAttribute("step", STEP_REQUEST);
            model.addAttribute("status", e.getMessage());
            model.addAttribute("statusColor", STATUS_COLOR_RED);
            return "auth/password-reset";
        }
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String login,
                          @Valid @ModelAttribute("verifyForm") PasswordResetVerifyForm verifyForm,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestForm", new PasswordResetRequestForm());
            model.addAttribute("step", STEP_VERIFY);
            model.addAttribute("login", login);
            model.addAttribute("maskedEmail", passwordResetService.maskEmail(login));
            return "auth/password-reset";
        }

        try {
            passwordResetService.resetPassword(
                    login,
                    verifyForm.getCode(),
                    verifyForm.getNewPassword(),
                    verifyForm.getRepeatPassword()
            );

            return "redirect:" + "/auth" + "/login" + "?resetSuccess=true";
        } catch (RuntimeException e) {
            model.addAttribute("requestForm", new PasswordResetRequestForm());
            model.addAttribute("step", STEP_VERIFY);
            model.addAttribute("login", login);
            model.addAttribute("maskedEmail", passwordResetService.maskEmail(login));
            model.addAttribute("status", e.getMessage());
            model.addAttribute("statusColor", STATUS_COLOR_RED);
            return "auth/password-reset";
        }
    }
}