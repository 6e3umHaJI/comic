package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.AdminComicForm;
import by.bsuir.springbootproject.services.AdminComicManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/comics")
@RequiredArgsConstructor
public class AdminComicManageController {

    private final AdminComicManageService adminComicManageService;

    @GetMapping("/new")
    public ModelAndView createPage() {
        return adminComicManageService.getCreatePage(null, null);
    }

    @GetMapping("/{id}/edit")
    public ModelAndView editPage(@PathVariable Integer id) {
        return adminComicManageService.getEditPage(id, null, null);
    }

    @PostMapping("/save")
    public ModelAndView saveComic(@ModelAttribute("form") AdminComicForm form,
                                  @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        try {
            Integer comicId = adminComicManageService.saveComic(form, coverFile);
            return new ModelAndView("redirect:/comics/" + comicId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return form.getComicId() != null
                    ? adminComicManageService.getEditPage(form.getComicId(), form, e.getMessage())
                    : adminComicManageService.getCreatePage(form, e.getMessage());
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public List<Map<String, Object>> searchComics(@RequestParam(defaultValue = "") String q,
                                                  @RequestParam(required = false) Integer excludeComicId) {
        return adminComicManageService.searchComics(q, excludeComicId);
    }
}
