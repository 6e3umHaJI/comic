package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContinueReadingInfo {
    private final Integer translationId;
    private final Integer chapterNumber;
    private final String languageName;
    private final Integer currentPage;
}
