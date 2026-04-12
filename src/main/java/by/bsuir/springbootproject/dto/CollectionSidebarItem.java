package by.bsuir.springbootproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CollectionSidebarItem {
    private Integer id;
    private String name;
    private Boolean isDefault;
    private long comicsCount;
}
