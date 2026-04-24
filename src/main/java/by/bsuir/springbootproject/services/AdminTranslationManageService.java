package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.AdminTranslationEditForm;
import by.bsuir.springbootproject.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MultipartHttpServletRequest;

public interface AdminTranslationManageService {
    ModelAndView getEditPage(Integer translationId, User admin, AdminTranslationEditForm form, String errorMessage);

    Integer updateTranslation(Integer translationId,
                              User admin,
                              AdminTranslationEditForm form,
                              String pagesPayload,
                              MultipartHttpServletRequest multipartRequest);

    Integer deleteTranslation(Integer translationId, User admin);
}
