package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.dto.*;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.CollectionService;
import by.bsuir.springbootproject.services.ComicService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/collections")
public class CollectionsController {

    private final CollectionService collectionService;
    private final SecurityContextUtils securityContextUtils;
    private final ComicService comicService;

    @GetMapping
    public ModelAndView page(@RequestParam(required = false) Integer sectionId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "card") String viewMode,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "addedAt") String sortField,
                             @RequestParam(defaultValue = "desc") String sortDirection,
                             @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        ModelAndView mv = collectionService.getCollectionsPage(
                user.getId(),
                sectionId,
                page,
                viewMode,
                q,
                sortField,
                sortDirection
        );

        mv.setViewName("XMLHttpRequest".equals(requestedWith)
                ? "collections/collections-content"
                : "collections/collections-page");

        return mv;
    }


    @GetMapping("/comic-modal")
    public ModelAndView comicModal(@RequestParam Integer comicId) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        ModelAndView mv = collectionService.getComicModal(user.getId(), comicId);
        mv.setViewName("collections/collection-comic-modal-content");
        return mv;
    }

    @PostMapping("/comic-sync")
    @ResponseBody
    public Map<String, Object> syncComicCollections(@RequestParam Integer comicId,
                                                    @RequestParam(required = false) List<Integer> sectionIds) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        try {
            collectionService.syncComicCollections(user.getId(), comicId, sectionIds);
            boolean inCollections = collectionService.isComicInCollections(user.getId(), comicId);

            return Map.of(
                    "success", true,
                    "inCollections", inCollections,
                    "favoriteStats", comicService.getFavoriteStats(comicId),
                    "message", inCollections ? "Коллекция обновлена" : "Комикс удалён из всех разделов"
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }


    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@ModelAttribute CollectionCreateForm form) {
        User user = securityContextUtils.getUserFromContext()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        try {
            collectionService.createSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория создана");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/rename")
    @ResponseBody
    public Map<String, Object> rename(@ModelAttribute CollectionRenameForm form) {
        try {
            User user = securityContextUtils.getUserFromContext()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.renameSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория переименована");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@ModelAttribute CollectionDeleteForm form) {
        try {
            User user = securityContextUtils.getUserFromContext()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.deleteSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория удалена");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/move")
    @ResponseBody
    public Map<String, Object> move(@ModelAttribute CollectionMoveForm form) {
        try {
            User user = securityContextUtils.getUserFromContext()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.moveComics(user.getId(), form);
            return Map.of("success", true, "message", "Тайтлы перенесены");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public Map<String, Object> remove(@ModelAttribute CollectionRemoveForm form) {
        try {
            User user = securityContextUtils.getUserFromContext()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.removeComics(user.getId(), form);
            return Map.of("success", true, "message", "Тайтлы удалены из категории");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
