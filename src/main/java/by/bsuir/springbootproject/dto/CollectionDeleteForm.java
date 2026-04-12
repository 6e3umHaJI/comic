package by.bsuir.springbootproject.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollectionDeleteForm {
    private Integer sectionId;
    private List<Integer> targetSectionIds;
    private Boolean deleteComics;
}
