package by.bsuir.springbootproject.controllers;

import by.bsuir.springbootproject.constants.RoutePaths;
import by.bsuir.springbootproject.constants.ViewPaths;
import by.bsuir.springbootproject.dto.*;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.services.CollectionService;
import by.bsuir.springbootproject.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping(RoutePaths.COLLECTIONS)
public class CollectionsController {

    private final CollectionService collectionService;

    @GetMapping
    public ModelAndView page(@RequestParam(required = false) Integer sectionId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "card") String viewMode,
                             @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        User user = SecurityContextUtils.getUser()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        ModelAndView mv = collectionService.getCollectionsPage(user.getId(), sectionId, page, viewMode);
        mv.setViewName("XMLHttpRequest".equals(requestedWith)
                ? ViewPaths.COLLECTIONS_CONTENT
                : ViewPaths.COLLECTIONS_PAGE);
        return mv;
    }

    @GetMapping(RoutePaths.COLLECTIONS_COMIC_MODAL)
    public ModelAndView comicModal(@RequestParam Integer comicId) {
        User user = SecurityContextUtils.getUser()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        ModelAndView mv = collectionService.getComicModal(user.getId(), comicId);
        mv.setViewName(ViewPaths.COLLECTIONS_COMIC_MODAL);
        return mv;
    }

    @PostMapping(RoutePaths.COLLECTIONS_COMIC_SYNC)
    @ResponseBody
    public Map<String, Object> syncComicCollections(@RequestParam Integer comicId,
                                                    @RequestParam(required = false) List<Integer> sectionIds) {
        User user = SecurityContextUtils.getUser()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        try {
            collectionService.syncComicCollections(user.getId(), comicId, sectionIds);
            boolean inCollections = collectionService.isComicInCollections(user.getId(), comicId);

            return Map.of(
                    "success", true,
                    "inCollections", inCollections,
                    "message", inCollections
                            ? "Коллекция обновлена"
                            : "Комикс удалён из всех разделов"
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping(RoutePaths.COLLECTIONS_CREATE)
    @ResponseBody
    public Map<String, Object> create(@ModelAttribute CollectionCreateForm form) {
        User user = SecurityContextUtils.getUser()
                .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

        try {
            collectionService.createSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория создана");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping(RoutePaths.COLLECTIONS_RENAME)
    @ResponseBody
    public Map<String, Object> rename(@ModelAttribute CollectionRenameForm form) {
        try {
            User user = SecurityContextUtils.getUser()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.renameSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория переименована");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping(RoutePaths.COLLECTIONS_DELETE)
    @ResponseBody
    public Map<String, Object> delete(@ModelAttribute CollectionDeleteForm form) {
        try {
            User user = SecurityContextUtils.getUser()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.deleteSection(user.getId(), form);
            return Map.of("success", true, "message", "Категория удалена");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping(RoutePaths.COLLECTIONS_MOVE)
    @ResponseBody
    public Map<String, Object> move(@ModelAttribute CollectionMoveForm form) {
        try {
            User user = SecurityContextUtils.getUser()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.moveComics(user.getId(), form);
            return Map.of("success", true, "message", "Тайтлы перенесены");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping(RoutePaths.COLLECTIONS_REMOVE)
    @ResponseBody
    public Map<String, Object> remove(@ModelAttribute CollectionRemoveForm form) {
        try {
            User user = SecurityContextUtils.getUser()
                    .orElseThrow(() -> new RuntimeException("Пользователь не авторизован"));

            collectionService.removeComics(user.getId(), form);
            return Map.of("success", true, "message", "Тайтлы удалены из категории");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
