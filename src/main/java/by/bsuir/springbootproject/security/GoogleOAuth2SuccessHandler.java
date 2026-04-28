package by.bsuir.springbootproject.security;

import by.bsuir.springbootproject.dto.PendingGoogleRegistration;
import by.bsuir.springbootproject.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    public static final String PENDING_GOOGLE_REGISTRATION_SESSION_KEY = "PENDING_GOOGLE_REGISTRATION";

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser oidcUser)) {
            response.sendRedirect(request.getContextPath() + "/auth/login?oauthError=true");
            return;
        }

        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();
        String subject = oidcUser.getSubject();
        String fullName = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        if (email == null || email.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/auth/login?oauthError=true");
            return;
        }

        if (userRepository.existsByEmail(email)) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(
                PENDING_GOOGLE_REGISTRATION_SESSION_KEY,
                PendingGoogleRegistration.builder()
                        .googleSubject(subject)
                        .email(email)
                        .emailVerified(emailVerified)
                        .displayName(fullName)
                        .avatarUrl(picture)
                        .build()
        );

        response.sendRedirect(request.getContextPath() + "/auth/register-google");
    }
}