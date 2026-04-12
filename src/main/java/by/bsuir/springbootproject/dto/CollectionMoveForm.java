package by.bsuir.springbootproject.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionMoveForm {

    @NotNull
    private Integer fromSectionId;

    @NotNull
    private Integer toSectionId;

    @NotEmpty
    private List<Integer> comicIds;
}
