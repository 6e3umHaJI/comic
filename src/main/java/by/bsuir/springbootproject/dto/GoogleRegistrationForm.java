package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleRegistrationForm {

    @NotBlank(message = "Введите никнейм")
    @Size(min = 3, max = 30, message = "Никнейм должен быть от 3 до 30 символов.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{3,30}$",
            message = "Никнейм может содержать только латинские буквы, цифры, дефис и подчёркивание."
    )
    private String username;

    @NotBlank(message = "Введите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{8,72}$",
            message = "Пароль может содержать только латинские буквы, цифры, дефис и подчёркивание."
    )
    private String password;

    @NotBlank(message = "Повторите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{8,72}$",
            message = "Пароль может содержать только латинские буквы, цифры, дефис и подчёркивание."
    )
    private String confirmPassword;
}