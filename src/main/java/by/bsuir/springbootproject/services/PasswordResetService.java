package by.bsuir.springbootproject.services;

public interface PasswordResetService {
    String sendCode(String login);
    void resetPassword(String login, String code, String newPassword, String repeatPassword);
    String maskEmail(String login);
}