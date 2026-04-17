package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.GoogleRegistrationForm;
import by.bsuir.springbootproject.dto.PendingGoogleRegistration;
import by.bsuir.springbootproject.security.GoogleOAuth2SuccessHandler;
import by.bsuir.springbootproject.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        model.addAttribute("isLogged", request.getUserPrincipal() != null);
        return "auth/login";
    }

    @GetMapping("/register-google")
    public String registerGooglePage(HttpSession session, Model model) {
        PendingGoogleRegistration pending = (PendingGoogleRegistration)
                session.getAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);

        if (pending == null) {
            return "redirect:" + "/auth/login";
        }

        model.addAttribute("pendingGoogle", pending);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new GoogleRegistrationForm());
        }
        return "auth/register-google";
    }

    @PostMapping("/register-google")
    public String registerGoogleSubmit(@Valid @ModelAttribute("form") GoogleRegistrationForm form,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       Model model) {
        PendingGoogleRegistration pending = (PendingGoogleRegistration)
                session.getAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);

        if (pending == null) {
            return "redirect:" + "/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pendingGoogle", pending);
            return "auth/register-google";
        }

        try {
            authService.completeGoogleRegistration(pending, form);
            session.removeAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);
            session.setAttribute("authSuccessMessage", "Аккаунт создан. Теперь войдите по логину и паролю.");
            return "redirect:" + "/auth/login" + "?registered=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("pendingGoogle", pending);
            model.addAttribute("registerError", e.getMessage());
            return "auth/register-google";
        }
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "redirect:" + "/home";
    }

    @GetMapping("/access-required")
    public String accessRequiredModalPage() {
        return "auth/auth-required-modal";
    }

    @ModelAttribute("authPrincipal")
    public Authentication authPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}