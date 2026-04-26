package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AutoTranslationService {

    Map<String, Object> buildPreview(Integer comicId,
                                     User admin,
                                     TranslationSubmissionForm form,
                                     MultipartFile[] pageFiles,
                                     List<Integer> selectedPageNumbers,
                                     String contextPath);

    List<ComicPage> materializePreview(Translation translation,
                                       String previewToken,
                                       User admin);
}
