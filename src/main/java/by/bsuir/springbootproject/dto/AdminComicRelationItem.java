package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminComicRelationItem {
    private Integer relatedComicId;
    private String relatedComicTitle;
    private String relationTypeName;
}
