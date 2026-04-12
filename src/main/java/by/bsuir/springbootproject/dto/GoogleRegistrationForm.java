package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleRegistrationForm {

    @NotBlank(message = "Введите имя пользователя")
    @Size(min = 3, max = 30, message = "Имя пользователя должно быть от 3 до 30 символов")
    @Pattern(
            regexp = "^[A-Za-z0-9_]+$",
            message = "Имя пользователя может содержать только латиницу, цифры и _"
    )
    private String username;

    @NotBlank(message = "Введите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
    private String password;

    @NotBlank(message = "Подтвердите пароль")
    private String confirmPassword;
}