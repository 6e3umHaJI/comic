package by.bsuir.springbootproject.principal;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.util.List;
import java.util.Locale;

public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer userId;
    private final String displayUsername;
    private final String roleName;
    private final Boolean canPropose;

    public UserPrincipal(User user) {
        super(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(toSpringRole(user)))
        );

        this.userId = user.getId();
        this.displayUsername = user.getUsername();
        this.roleName = user.getRole() != null ? user.getRole().getName() : "USER";
        this.canPropose = user.getCanPropose();
    }

    public Integer getUserId() {
        return userId;
    }

    public String getDisplayUsername() {
        return displayUsername;
    }

    public String getRoleName() {
        return roleName;
    }

    public Boolean getCanPropose() {
        return canPropose;
    }

    public User getUser() {
        UserRole role = UserRole.builder()
                .name(roleName)
                .build();

        return User.builder()
                .id(userId)
                .email(getUsername())
                .passwordHash(getPassword())
                .username(displayUsername)
                .role(role)
                .canPropose(canPropose)
                .build();
    }

    private static String toSpringRole(User user) {
        String roleName = user != null && user.getRole() != null && user.getRole().getName() != null
                ? user.getRole().getName().trim().toUpperCase(Locale.ROOT)
                : "USER";

        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
    }
}
