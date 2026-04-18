package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.NotificationToggleResult;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Translation;
import org.springframework.web.servlet.ModelAndView;

public interface NotificationService {

    ModelAndView getNotificationsPage(Integer userId,
                                      String tab,
                                      int page,
                                      String q,
                                      String sortField,
                                      String sortDirection);

    long getNotificationCount(Integer userId);

    long getUnreadNotificationCount(Integer userId);

    boolean isComicSubscribed(Integer userId, Integer comicId);

    NotificationToggleResult toggleComicSubscription(Integer userId, Integer comicId);

    boolean deleteNotification(Integer userId, Integer notificationId);

    void notifyNewTranslationForSubscribers(Translation translation);

    void notifyNewChapterForSubscribers(Chapter chapter);

    void notifyChapterApproved(Integer userId, Translation translation);

    void notifyChapterRejected(Integer userId, Translation translation, String message);

    void notifyAdminMessage(Integer userId, String message);

    void notifyComicRemovedFromCollections(Integer userId, String comicTitle);

    void notifyUserChapterDeleted(Integer userId,
                                  Integer comicId,
                                  String comicTitle,
                                  Integer chapterNumber,
                                  String languageName,
                                  String message);

    void notifyUploadRightsRevoked(Integer userId, String message);

    void notifyComplaintReviewed(Integer userId,
                                 Integer comicId,
                                 String comicTitle,
                                 String message);
}
