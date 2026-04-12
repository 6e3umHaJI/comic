package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestForm {

    @NotBlank(message = "Введите email или никнейм")
    private String login;
}