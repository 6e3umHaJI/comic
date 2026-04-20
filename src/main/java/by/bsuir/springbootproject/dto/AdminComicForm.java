package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminComicForm {
    private Integer comicId;
    private String currentCover;
    private String title;
    private String originalTitle;
    private String releaseYear;
    private String shortDescription;
    private String fullDescription;
    private Integer typeId;
    private Integer ageRatingId;
    private Integer comicStatusId;

    @Builder.Default
    private List<Integer> genreIds = new ArrayList<>();

    @Builder.Default
    private List<Integer> tagIds = new ArrayList<>();

    @Builder.Default
    private String genreOperationsJson = "[]";

    @Builder.Default
    private String tagOperationsJson = "[]";

    @Builder.Default
    private String relationTypeOperationsJson = "[]";

    @Builder.Default
    private String relationsJson = "[]";

    @Builder.Default
    private List<AdminComicRelationItem> relationItems = new ArrayList<>();
}
