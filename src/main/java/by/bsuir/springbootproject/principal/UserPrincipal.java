package by.bsuir.springbootproject.principal;

import by.bsuir.springbootproject.entities.UserRole;
import by.bsuir.springbootproject.entities.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


public class UserPrincipal extends org.springframework.security.core.userdetails.User {
    private final User user;

    public UserPrincipal(User user) {
        super(user.getEmail(), user.getPasswordHash(), java.util.List.of(new SimpleGrantedAuthority(user.getRole().getName())));
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
