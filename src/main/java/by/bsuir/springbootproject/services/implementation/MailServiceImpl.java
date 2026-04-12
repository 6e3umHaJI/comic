package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.reset.code-expiration-minutes}")
    private int expirationMinutes;

    @Override
    public void sendPasswordResetCode(String to, String username, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Восстановление пароля");
        message.setText("""
                Здравствуйте, %s!

                Ваш код для восстановления пароля: %s

                Код действует %d минут.
                Если это были не вы, просто проигнорируйте это письмо.
                """.formatted(username, code, expirationMinutes));

        mailSender.send(message);
    }
}