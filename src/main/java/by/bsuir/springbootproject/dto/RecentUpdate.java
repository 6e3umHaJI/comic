package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecentUpdate {
    private final Integer translationId;
    private final Integer comicId;
    private final String comicTitle;
    private final String comicCover;
    private final Integer chapterNumber;
    private final String languageName;
    private final String createdAtFormatted;
    private final String createdAtIso;
}
