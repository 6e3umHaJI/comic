package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
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
@RequestMapping(RoutePaths.AUTH)
public class AuthController {

    private final AuthService authService;

    @GetMapping(RoutePaths.AUTH_LOGIN)
    public String loginPage(HttpServletRequest request, Model model) {
        model.addAttribute("isLogged", request.getUserPrincipal() != null);
        return ViewPaths.LOGIN;
    }

    @GetMapping(RoutePaths.AUTH_REGISTER_GOOGLE)
    public String registerGooglePage(HttpSession session, Model model) {
        PendingGoogleRegistration pending = (PendingGoogleRegistration)
                session.getAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);

        if (pending == null) {
            return "redirect:" + RoutePaths.LOGIN;
        }

        model.addAttribute("pendingGoogle", pending);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new GoogleRegistrationForm());
        }
        return ViewPaths.REGISTER_GOOGLE;
    }

    @PostMapping(RoutePaths.AUTH_REGISTER_GOOGLE)
    public String registerGoogleSubmit(@Valid @ModelAttribute("form") GoogleRegistrationForm form,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       Model model) {
        PendingGoogleRegistration pending = (PendingGoogleRegistration)
                session.getAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);

        if (pending == null) {
            return "redirect:" + RoutePaths.LOGIN;
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pendingGoogle", pending);
            return ViewPaths.REGISTER_GOOGLE;
        }

        try {
            authService.completeGoogleRegistration(pending, form);
            session.removeAttribute(GoogleOAuth2SuccessHandler.PENDING_GOOGLE_REGISTRATION_SESSION_KEY);
            session.setAttribute("authSuccessMessage", "Аккаунт создан. Теперь войдите по логину и паролю.");
            return "redirect:" + RoutePaths.LOGIN + "?registered=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("pendingGoogle", pending);
            model.addAttribute("registerError", e.getMessage());
            return ViewPaths.REGISTER_GOOGLE;
        }
    }

    @GetMapping(RoutePaths.AUTH_LOGOUT_SUCCESS)
    public String logoutSuccess() {
        return "redirect:" + RoutePaths.HOME;
    }

    @GetMapping(RoutePaths.AUTH_ACCESS_REQUIRED)
    public String accessRequiredModalPage() {
        return ViewPaths.ACCESS_REQUIRED;
    }

    @ModelAttribute("authPrincipal")
    public Authentication authPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}