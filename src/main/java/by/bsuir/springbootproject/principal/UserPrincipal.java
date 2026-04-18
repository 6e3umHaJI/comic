package by.bsuir.springbootproject.principal;

import by.bsuir.springbootproject.entities.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Locale;

public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final User user;

    public UserPrincipal(User user) {
        super(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(toSpringRole(user)))
        );
        this.user = user;
    }

    private static String toSpringRole(User user) {
        String roleName = user != null
                && user.getRole() != null
                && user.getRole().getName() != null
                ? user.getRole().getName().trim().toUpperCase(Locale.ROOT)
                : "USER";

        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
    }

    public User getUser() {
        return user;
    }
}
