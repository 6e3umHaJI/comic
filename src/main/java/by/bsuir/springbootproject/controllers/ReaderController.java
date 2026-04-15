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
import by.bsuir.springbootproject.services.CollectionService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import by.bsuir.springbootproject.repositories.TranslationRepository;

import java.util.List;
import java.util.Objects;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(RoutePaths.READ)
public class ReaderController {

    private final ReaderService readerService;
    private final CollectionService collectionService;
    private final SecurityContextUtils securityContextUtils;
    private final TranslationRepository translationRepository;

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

        boolean isLogged = request.getUserPrincipal() != null;
        boolean inCollections = false;

        if (isLogged) {
            Integer userId = securityContextUtils.getUserFromContext()
                    .map(by.bsuir.springbootproject.entities.User::getId)
                    .orElse(null);

            if (userId != null) {
                inCollections = collectionService.isComicInCollections(userId, data.getComic().getId());
            }
        }

        model.addAttribute("comic", data.getComic());
        model.addAttribute("translation", data.getTranslation());
        model.addAttribute("pages", data.getPages());
        model.addAttribute("prevTranslation", data.getPrevTranslation());
        model.addAttribute("nextTranslation", data.getNextTranslation());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("initialPage", initialPage);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("inCollections", inCollections);

        return ViewPaths.READER_PAGE;
    }


    @PostMapping("/{translationId}/progress")
    @ResponseBody
    public ResponseEntity<?> saveProgress(@PathVariable Integer translationId,
                                          @RequestParam Integer page) {
        readerService.saveProgressIfAuthenticated(translationId, page);
        return ResponseEntity.ok(Map.of("status", "ok", "page", page));
    }

    @GetMapping("/chapters/{chapterId}/languages")
    @ResponseBody
    public Map<String, Object> getChapterLanguages(@PathVariable Integer chapterId) {
        List<String> languages = translationRepository.findApprovedLangsByChapterIds(List.of(chapterId)).stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == chapterId)
                .map(row -> (String) row[1])
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return Map.of(
                "chapterId", chapterId,
                "languages", languages
        );
    }

}
