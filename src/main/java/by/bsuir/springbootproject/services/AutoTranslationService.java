package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AutoTranslationService {

    List<ComicPage> generateTranslationPages(
            Translation translation,
            Integer sourceLanguageId,
            User admin,
            MultipartFile[] pageFiles,
            List<Integer> selectedPageNumbers
    );
}
