package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminComplaintItem {
    private final Integer id;
    private final String scope;
    private final String typeName;
    private final Integer statusId;
    private final String statusName;
    private final String description;
    private final String createdAtFormatted;
    private final String targetUrl;
    private final String targetTitle;
    private final String targetSubtitle;
    private final String cover;
    private final String username;
    private final String email;
}
