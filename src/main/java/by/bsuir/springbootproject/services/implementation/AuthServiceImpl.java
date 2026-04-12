package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.GoogleRegistrationForm;
import by.bsuir.springbootproject.dto.PendingGoogleRegistration;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserRole;
import by.bsuir.springbootproject.entities.UserSection;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.repositories.UserRoleRepository;
import by.bsuir.springbootproject.repositories.UserSectionRepository;
import by.bsuir.springbootproject.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSectionRepository userSectionRepository;

    @Override
    @Transactional
    public User completeGoogleRegistration(PendingGoogleRegistration pending, GoogleRegistrationForm form) {
        if (pending == null) {
            throw new IllegalStateException("Сессия регистрации через Google истекла. Войдите через Google ещё раз.");
        }

        if (pending.getEmail() == null || pending.getEmail().isBlank()) {
            throw new IllegalStateException("Google не вернул email.");
        }

        if (!Boolean.TRUE.equals(pending.getEmailVerified())) {
            throw new IllegalStateException("Email Google-аккаунта не подтвержден.");
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают.");
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            throw new IllegalArgumentException("Пользователь с такой почтой уже существует.");
        }

        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Такой никнейм уже занят.");
        }

        UserRole userRole = userRoleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Роль USER не найдена. Проверь таблицу user_roles."));

        User user = User.builder()
                .email(pending.getEmail())
                .username(form.getUsername())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .role(userRole)
                .canPropose(false)
                .build();

        User savedUser = userRepository.save(user);
        createDefaultSections(savedUser);
        return savedUser;

    }

    private void createDefaultSections(User user) {
        List<String> defaults = List.of("Читаю", "Любимые", "В планах", "Прочитано");
        for (String name : defaults) {
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