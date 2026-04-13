package by.bsuir.springbootproject.entities;

import lombok.*;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SearchCriteria {
    private String keyWords;
    private String sortField;
    private String sortDirection;
    private int pageNumber;
    private int pageSize;
    private String viewMode;

    private String[] selectedTypes = new String[0];
    private String[] selectedLanguages = new String[0];
    private String[] selectedComicStatuses = new String[0];
    private String[] selectedAgeRatings = new String[0];
    private String[] selectedGenres = new String[0];
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
