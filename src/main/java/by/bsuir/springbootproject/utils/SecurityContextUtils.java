package by.bsuir.springbootproject.utils;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.principal.UserPrincipal;
import by.bsuir.springbootproject.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityContextUtils {

    private final UserRepository userRepository;

    public Optional<User> getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.ofNullable(userPrincipal.getUser());
        }

        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            return userRepository.findByEmail(email);
        }

        return Optional.empty();
    }

    public static Optional<User> getUser() {
        throw new IllegalStateException(
                "Use injected SecurityContextUtils#getUserFromContext() instead of static getUser()."
        );
    }
}
