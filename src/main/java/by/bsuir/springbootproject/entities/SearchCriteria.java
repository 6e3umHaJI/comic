package by.bsuir.springbootproject.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SearchCriteria implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyWords;
    private String sortField;
    private String sortDirection;
    private int pageNumber;
    private int pageSize;
    private String viewMode;

    @Builder.Default
    private String[] selectedTypes = new String[0];

    @Builder.Default
    private String[] selectedLanguages = new String[0];

    @Builder.Default
    private String[] selectedComicStatuses = new String[0];

    @Builder.Default
    private String[] selectedAgeRatings = new String[0];

    @Builder.Default
    private String[] selectedGenres = new String[0];

    @Builder.Default
    private String[] selectedTags = new String[0];

    private boolean strictGenreMatch;
    private boolean strictTagMatch;
    private boolean strictLanguageMatch;

    private Integer releaseYearFrom;
    private Integer releaseYearTo;

    private Integer ratingsCountFrom;
    private Integer ratingsCountTo;

    private Double avgRatingFrom;
    private Double avgRatingTo;

    private Integer chaptersCountFrom;
    private Integer chaptersCountTo;

    private LocalDate updatedFrom;
    private LocalDate updatedTo;

    public void reset() {
        keyWords = "";
        selectedTypes = new String[0];
        selectedLanguages = new String[0];
        selectedComicStatuses = new String[0];
        selectedAgeRatings = new String[0];
        selectedGenres = new String[0];
        selectedTags = new String[0];

        strictGenreMatch = false;
        strictTagMatch = false;
        strictLanguageMatch = false;

        releaseYearFrom = null;
        releaseYearTo = null;
        ratingsCountFrom = null;
        ratingsCountTo = null;
        avgRatingFrom = null;
        avgRatingTo = null;
        chaptersCountFrom = null;
        chaptersCountTo = null;
        updatedFrom = null;
        updatedTo = null;
    }
}
