package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetVerifyForm {

    @NotBlank(message = "Введите код")
    @Size(min = 6, max = 6, message = "Код должен состоять из 6 символов")
    private String code;

    @NotBlank(message = "Введите новый пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
    private String newPassword;

    @NotBlank(message = "Повторите пароль")
    private String repeatPassword;
}