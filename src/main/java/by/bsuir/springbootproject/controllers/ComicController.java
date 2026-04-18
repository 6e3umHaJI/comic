package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.services.CollectionService;
import by.bsuir.springbootproject.services.ComplaintService;
import by.bsuir.springbootproject.services.ComicService;
import by.bsuir.springbootproject.services.NotificationService;
import by.bsuir.springbootproject.services.ReaderService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/comics")
public class ComicController {

    private static final String TAB_DESCRIPTION = "description";
    private static final String TAB_CHAPTERS = "chapters";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private final ComicService comicService;
    private final CollectionService collectionService;
    private final ComplaintService complaintService;
    private final SecurityContextUtils securityContextUtils;
    private final ReaderService readerService;
    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public String showComic(@PathVariable Integer id,
                            @RequestParam(defaultValue = TAB_DESCRIPTION) String tab,
                            HttpServletRequest request,
                            Model model) {
        Comic comic = comicService.getComicById(id);

        model.addAttribute("comic", comic);
        model.addAttribute("tab", tab);
        model.addAttribute("isLogged", request.getUserPrincipal() != null);
        model.addAttribute("chaptersPageSize", Values.CHAPTERS_PAGE_SIZE);

        model.addAllAttributes(comicService.getStartReadingData(id));
        model.addAttribute("continueReading", readerService.getContinueReadingInfoIfAuthenticated(id));
        model.addAttribute("complaintTypes", complaintService.getComplaintTypesForScope("COMIC"));
        if (request.getUserPrincipal() != null) {
            Integer userId = securityContextUtils.getUserFromContext()
                    .map(by.bsuir.springbootproject.entities.User::getId)
                    .orElse(null);

            if (userId != null) {
                boolean inCollections = collectionService.isComicInCollections(userId, id);
                boolean isNotificationsEnabled = notificationService.isComicSubscribed(userId, id);

                model.addAttribute("inCollections", inCollections);
                model.addAttribute("isNotificationsEnabled", isNotificationsEnabled);
            } else {
                model.addAttribute("inCollections", false);
                model.addAttribute("isNotificationsEnabled", false);
            }
        } else {
            model.addAttribute("inCollections", false);
            model.addAttribute("isNotificationsEnabled", false);
        }

        if (!TAB_CHAPTERS.equals(tab)) {
            model.addAttribute("relatedComics", comicService.getRelatedComics(id));
            model.addAttribute("relatedCount", comicService.getRelatedComicsCount(id));
            model.addAttribute("similarComics", comicService.getSimilarComics(id));
            model.addAttribute("ratingDistribution", comicService.getRatingDistribution(id));
            model.addAttribute("favoriteStats", comicService.getFavoriteStats(id));
            model.addAttribute("approvedLangStats", comicService.getApprovedLangStatsByComic(id));
        }

        boolean ajax = XML_HTTP_REQUEST.equals(request.getHeader("X-Requested-With"));
        return ajax ? "comic/comic-tab-content" : "comic/comic-page";
    }

    @GetMapping("/{id}/live-stats")
    @ResponseBody
    public Map<String, Object> getLiveStats(@PathVariable Integer id) {
        Comic comic = comicService.getComicById(id);

        return Map.of(
                "avgRating", comic.getAvgRating() != null ? comic.getAvgRating() : 0,
                "ratingsCount", comic.getRatingsCount() != null ? comic.getRatingsCount() : 0L,
                "ratingDistribution", comicService.getRatingDistribution(id),
                "favoriteStats", comicService.getFavoriteStats(id)
        );
    }

    @GetMapping("/{id}/related")
    public String getRelatedPaged(@PathVariable Integer id,
                                  @RequestParam(defaultValue = "0") int page,
                                  Model model) {
        model.addAttribute("related", comicService.getRelatedComicsWithTypePaged(id, page, Values.RELATED_PAGE_SIZE));
        return "comic/related-list";
    }

    @GetMapping("/{comicId}/chapters")
    public String getChaptersPageChunk(@PathVariable Integer comicId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestParam(defaultValue = "desc") String dir,
                                       @RequestParam(defaultValue = "") String q,
                                       Model model) {
        model.addAllAttributes(comicService.getChaptersChunkData(comicId, page, size, dir, q));
        return "comic/chapter-list";
    }

    @GetMapping("/chapters/{chapterId}/translations")
    public String getTranslationsChunk(@PathVariable Integer chapterId,
                                       @RequestParam(defaultValue = "") String lang,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       Model model) {
        Map<String, Object> translationChunkData = comicService.getTranslationsChunkData(chapterId, lang, page, size);
        model.addAllAttributes(translationChunkData);

        @SuppressWarnings("unchecked")
        List<Integer> translationIds = (List<Integer>) translationChunkData.get("translationIds");

        model.addAttribute("readTranslationIds", readerService.getReadTranslationIdsIfAuthenticated(translationIds));
        return "comic/translation-items";
    }
}
