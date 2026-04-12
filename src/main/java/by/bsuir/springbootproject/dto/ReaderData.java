package by.bsuir.springbootproject.dto;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReaderData {
    private final Comic comic;
    private final Translation translation;
    private final List<ComicPage> pages;
    private final Translation prevTranslation;
    private final Translation nextTranslation;
}