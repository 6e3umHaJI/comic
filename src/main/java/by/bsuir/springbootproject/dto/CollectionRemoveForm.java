package by.bsuir.springbootproject.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionRemoveForm {
    private Integer sectionId;
    private List<Integer> comicIds;
}
