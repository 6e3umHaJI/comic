package by.bsuir.springbootproject.dto;

import by.bsuir.springbootproject.entities.Comic;

public class RelatedWithType {
    private final Comic comic;
    private final String relationType;

    public RelatedWithType(Comic comic, String relationType) {
        this.comic = comic;
        this.relationType = relationType;
    }
    public Comic getComic() { return comic; }
    public String getRelationType() { return relationType; }
}