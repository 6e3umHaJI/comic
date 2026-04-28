package by.bsuir.springbootproject.security;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AuthInputValidationUtils {

    public static final int GMAIL_FULL_MAX_LENGTH = 40;
    public static final int GENERIC_EMAIL_MAX_LENGTH = 254;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{8,72}$");

    private static final Pattern GMAIL_LOCAL_PART_PATTERN =
            Pattern.compile("^(?=.{6,30}$)(?!\\.)(?!.*\\.\\.)([A-Za-z0-9.]+)(?<!\\.)$");

    private static final Pattern GENERIC_EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]{1,64}@[^\\s@]{1,190}\\.[^\\s@]{2,63}$");

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidUsername(String value) {
        String normalized = normalize(value);
        return USERNAME_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidPassword(String value) {
        String normalized = value == null ? "" : value;
        return PASSWORD_PATTERN.matcher(normalized).matches();
    }

    public static boolean isGmailAddress(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        return normalized.endsWith("@gmail.com");
    }

    public static boolean isValidGmailAddress(String value) {
        String normalized = normalize(value);
        if (normalized.length() > GMAIL_FULL_MAX_LENGTH || !isGmailAddress(normalized)) {
            return false;
        }

        String localPart = normalized.substring(0, normalized.length() - "@gmail.com".length());
        return GMAIL_LOCAL_PART_PATTERN.matcher(localPart).matches();
    }

    public static boolean isValidGenericEmail(String value) {
        String normalized = normalize(value);
        return normalized.length() <= GENERIC_EMAIL_MAX_LENGTH
                && GENERIC_EMAIL_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidLogin(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return false;
        }

        if (normalized.contains("@")) {
            if (isGmailAddress(normalized)) {
                return isValidGmailAddress(normalized);
            }
            return isValidGenericEmail(normalized);
        }

        return isValidUsername(normalized);
    }

    public static String getUsernameValidationMessage() {
        return "Никнейм должен быть от 3 до 30 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.";
    }

    public static String getPasswordValidationMessage() {
        return "Пароль должен быть от 8 до 72 символов и может содержать только латинские буквы, цифры, дефис и подчёркивание.";
    }

    public static String getLoginValidationMessage(String value) {
        String normalized = normalize(value);

        if (normalized.contains("@")) {
            if (isGmailAddress(normalized)) {
                return "Gmail должен содержать от 6 до 30 символов до @gmail.com. Допустимы латинские буквы, цифры и точка.";
            }
            return "Введите корректный email.";
        }

        return getUsernameValidationMessage();
    }
}
