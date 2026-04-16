package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationToggleResult {
    private final Integer comicId;
    private final boolean subscribed;
}
