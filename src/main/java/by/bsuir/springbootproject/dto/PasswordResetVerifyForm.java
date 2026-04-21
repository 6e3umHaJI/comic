package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetVerifyForm {

    @NotBlank(message = "Введите код")
    @Pattern(regexp = "^[0-9]{6}$", message = "Код должен состоять из 6 цифр.")
    private String code;

    @NotBlank(message = "Введите новый пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{8,72}$",
            message = "Пароль может содержать только латинские буквы, цифры, дефис и подчёркивание."
    )
    private String newPassword;

    @NotBlank(message = "Повторите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{8,72}$",
            message = "Пароль может содержать только латинские буквы, цифры, дефис и подчёркивание."
    )
    private String repeatPassword;
}