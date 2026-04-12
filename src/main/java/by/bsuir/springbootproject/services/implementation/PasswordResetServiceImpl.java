package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.PasswordResetCode;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.PasswordResetCodeRepository;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.services.MailService;
import by.bsuir.springbootproject.services.PasswordResetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.reset.code-expiration-minutes}")
    private int expirationMinutes;

    @Value("${app.reset.max-attempts}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String sendCode(String login) {
        User user = findUser(login)
                .orElseThrow(() -> new RuntimeException("Если такой аккаунт существует, код будет отправлен"));

        passwordResetCodeRepository.invalidateAllByUserId(user.getId());

        String code = generateCode();

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .attemptsCount(0)
                .used(false)
                .build();

        passwordResetCodeRepository.save(resetCode);
        mailService.sendPasswordResetCode(user.getEmail(), user.getUsername(), code);

        return maskEmailInternal(user.getEmail());
    }

    @Override
    public void resetPassword(String login, String code, String newPassword, String repeatPassword) {
        if (!newPassword.equals(repeatPassword)) {
            throw new RuntimeException("Пароли не совпадают");
        }

        User user = findUser(login)
                .orElseThrow(() -> new RuntimeException("Если такой аккаунт существует, код будет отправлен"));

        PasswordResetCode resetCode = passwordResetCodeRepository.findTopActiveByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Код не найден. Запросите новый"));

        if (Boolean.TRUE.equals(resetCode.getUsed())) {
            throw new RuntimeException("Код уже использован");
        }

        if (resetCode.isExpired()) {
            throw new RuntimeException("Срок действия кода истёк");
        }

        if (resetCode.getAttemptsCount() >= maxAttempts) {
            throw new RuntimeException("Превышено число попыток. Запросите новый код");
        }

        if (!passwordEncoder.matches(code, resetCode.getCodeHash())) {
            resetCode.setAttemptsCount(resetCode.getAttemptsCount() + 1);
            passwordResetCodeRepository.save(resetCode);
            throw new RuntimeException("Неверный код");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetCode.setUsed(true);
        passwordResetCodeRepository.save(resetCode);
    }

    @Override
    public String maskEmail(String login) {
        User user = findUser(login)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return maskEmailInternal(user.getEmail());
    }

    private Optional<User> findUser(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }

        String normalized = login.trim();

        if (normalized.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalized);
        }

        return userRepository.findByUsernameIgnoreCase(normalized);
    }

    private String generateCode() {
        int value = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(value);
    }

    private String maskEmailInternal(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at);

        String name = email.substring(0, at);
        String domain = email.substring(at);

        if (name.length() == 2) {
            return name.charAt(0) + "*" + domain;
        }

        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}