package by.bsuir.springbootproject.config;

import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserRole;
import by.bsuir.springbootproject.entities.UserSection;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.repositories.UserRoleRepository;
import by.bsuir.springbootproject.repositories.UserSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@comic.local";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin12345";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserSectionRepository userSectionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        UserRole adminRole = userRoleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Роль ADMIN не найдена."));

        User admin = userRepository.findByUsernameIgnoreCase(ADMIN_USERNAME)
                .or(() -> userRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .orElseGet(() -> User.builder().build());

        admin.setEmail(ADMIN_EMAIL);
        admin.setUsername(ADMIN_USERNAME);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(adminRole);
        admin.setCanPropose(true);

        User savedAdmin = userRepository.save(admin);
        createDefaultSections(savedAdmin);
    }

    private void createDefaultSections(User user) {
        List<String> defaults = List.of("Читаю", "Любимые", "В планах", "Прочитано");

        for (String name : defaults) {
            if (!userSectionRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
                userSectionRepository.save(
                        UserSection.builder()
                                .user(user)
                                .name(name)
                                .isDefault(true)
                                .build()
                );
            }
        }
    }
}
