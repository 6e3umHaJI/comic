package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationFeedItem {
    private final Integer id;
    private final String title;
    private final String subject;
    private final String details;
    private final String cover;
    private final String linkPath;
    private final boolean clickable;
    private final boolean read;
    private final String createdAtFormatted;
    private final String createdAtIso;
}
