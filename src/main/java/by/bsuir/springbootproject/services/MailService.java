package by.bsuir.springbootproject.services;

public interface MailService {
    void sendPasswordResetCode(String to, String username, String code);
}