package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.*;
import by.bsuir.springbootproject.entities.User;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

public interface CollectionService {

    ModelAndView getCollectionsPage(Integer userId, Integer sectionId, int page, String viewMode);

    ModelAndView getComicModal(Integer userId, Integer comicId);

    boolean isComicInCollections(Integer userId, Integer comicId);

    void syncComicCollections(Integer userId, Integer comicId, List<Integer> sectionIds);

    void createSection(Integer userId, CollectionCreateForm form);

    void renameSection(Integer userId, CollectionRenameForm form);

    void deleteSection(Integer userId, CollectionDeleteForm form);

    void moveComics(Integer userId, CollectionMoveForm form);

    void removeComics(Integer userId, CollectionRemoveForm form);

    void ensureDefaultSections(User user);
}
