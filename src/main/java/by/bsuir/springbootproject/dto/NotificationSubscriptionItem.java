package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSubscriptionItem {
    private final Integer comicId;
    private final String title;
    private final String originalTitle;
    private final String cover;
    private final Integer releaseYear;
    private final Double avgRating;
}
