package by.bsuir.springbootproject.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionMoveForm {
    private Integer fromSectionId;
    private Integer toSectionId;
    private List<Integer> comicIds;
}
