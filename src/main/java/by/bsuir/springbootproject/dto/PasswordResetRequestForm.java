package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestForm {

    @NotBlank(message = "Введите email или никнейм")
    @Size(max = 254, message = "Слишком длинный email или никнейм.")
    private String login;
}
