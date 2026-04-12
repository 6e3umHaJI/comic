package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionRenameForm {

    @NotNull
    private Integer sectionId;

    @NotBlank(message = "Введите новое название")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String name;
}
