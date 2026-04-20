package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.AdminComicForm;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

public interface AdminComicManageService {
    ModelAndView getCreatePage(AdminComicForm form, String errorMessage);

    ModelAndView getEditPage(Integer comicId, AdminComicForm form, String errorMessage);

    Integer saveComic(AdminComicForm form, MultipartFile coverFile);

    List<Map<String, Object>> searchComics(String q, Integer excludeComicId);
}
