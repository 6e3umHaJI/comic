package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuickSearchComicItem {
    private final Integer id;
    private final String title;
    private final String originalTitle;
    private final String shortDescription;
    private final String cover;
}
