package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
import by.bsuir.springbootproject.dto.ReaderData;
import by.bsuir.springbootproject.services.ReaderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(RoutePaths.READ)
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping("/{translationId}")
    public String openReader(@PathVariable Integer translationId,
                             @RequestParam(required = false) Integer page,
                             HttpServletRequest request,
                             Model model) {
        ReaderData data = readerService.getReaderData(translationId);

        readerService.markTranslationOpenedIfAuthenticated(translationId);
        readerService.markChapterReadIfAuthenticated(data.getTranslation().getChapter().getId());

        int totalPages = data.getPages().size();
        int initialPage = page != null
                ? Math.min(Math.max(page, 1), totalPages)
                : Math.min(Math.max(readerService.getSavedPageIfAuthenticated(translationId), 1), totalPages);

        model.addAttribute("comic", data.getComic());
        model.addAttribute("translation", data.getTranslation());
        model.addAttribute("pages", data.getPages());
        model.addAttribute("prevTranslation", data.getPrevTranslation());
        model.addAttribute("nextTranslation", data.getNextTranslation());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("initialPage", initialPage);
        model.addAttribute("isLogged", request.getUserPrincipal() != null);

        return ViewPaths.READER_PAGE;
    }

    @PostMapping("/{translationId}/progress")
    @ResponseBody
    public ResponseEntity<?> saveProgress(@PathVariable Integer translationId,
                                          @RequestParam Integer page) {
        readerService.saveProgressIfAuthenticated(translationId, page);
        return ResponseEntity.ok(Map.of("status", "ok", "page", page));
    }
}
