package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.GoogleRegistrationForm;
import by.bsuir.springbootproject.dto.PendingGoogleRegistration;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserRole;
import by.bsuir.springbootproject.entities.UserSection;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.repositories.UserRoleRepository;
import by.bsuir.springbootproject.repositories.UserSectionRepository;
import by.bsuir.springbootproject.security.AuthInputValidationUtils;
import by.bsuir.springbootproject.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        if (!AuthInputValidationUtils.isValidPassword(form.getPassword())) {
            throw new IllegalArgumentException(AuthInputValidationUtils.getPasswordValidationMessage());
        }

        if (!AuthInputValidationUtils.isValidPassword(form.getConfirmPassword())) {
            throw new IllegalArgumentException(AuthInputValidationUtils.getPasswordValidationMessage());
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают.");
        }

        String normalizedEmail = AuthInputValidationUtils.normalize(pending.getEmail());
        String normalizedUsername = AuthInputValidationUtils.normalize(form.getUsername());

        if (!AuthInputValidationUtils.isValidUsername(normalizedUsername)) {
            throw new IllegalArgumentException(AuthInputValidationUtils.getUsernameValidationMessage());
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Пользователь с такой почтой уже существует.");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Такой никнейм уже занят.");
        }


        UserRole userRole = userRoleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Роль USER не найдена. Проверь таблицу user_roles."));

        User user = User.builder()
                .email(normalizedEmail)
                .username(normalizedUsername)
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .role(userRole)
                .canPropose(false)
                .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            createDefaultSections(savedUser);
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Пользователь с такой почтой или никнеймом уже существует.");
        }

    }

    private void createDefaultSections(User user) {
        List<String> defaults = List.of("Читаю", "Любимые", "В планах", "Прочитано");

        for (String name : defaults) {
            if (userSectionRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
                continue;
            }

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
