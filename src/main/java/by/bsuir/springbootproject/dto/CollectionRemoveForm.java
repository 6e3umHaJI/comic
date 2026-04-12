package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionRemoveForm {

    @NotNull
    private Integer sectionId;

    @NotEmpty
    private List<Integer> comicIds;
}
