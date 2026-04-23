package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.TranslationSubmissionForm;
import by.bsuir.springbootproject.entities.User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface TranslationSubmissionService {
    ModelAndView getCreatePage(Integer comicId, User user, TranslationSubmissionForm form, String errorMessage);

    Map<String, Object> getChapterOptions(Integer comicId, Integer languageId);

    Integer submit(Integer comicId, User user, TranslationSubmissionForm form, MultipartFile[] pageFiles);

    ModelAndView getPreviewPage(Integer translationId, User viewer, String successMessage, String errorMessage);

    ModelAndView getModerationPage(int page);

    void approve(Integer translationId, User admin);

    void reject(Integer translationId, User admin, String reason, boolean revokeRights);
}
