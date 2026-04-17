package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.dto.NotificationFeedItem;
import by.bsuir.springbootproject.dto.NotificationSubscriptionItem;
import by.bsuir.springbootproject.dto.NotificationToggleResult;
import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicNotificationSubscription;
import by.bsuir.springbootproject.entities.Notification;
import by.bsuir.springbootproject.entities.NotificationType;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.repositories.ComicNotificationSubscriptionRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.NotificationRepository;
import by.bsuir.springbootproject.repositories.NotificationTypeRepository;
import by.bsuir.springbootproject.repositories.UserRepository;
import by.bsuir.springbootproject.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String TAB_FEED = "feed";
    private static final String TAB_SUBSCRIPTIONS = "subscriptions";

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_TYPE = "type";

    private static final String TYPE_NEW_TRANSLATION = "Добавлен новый перевод";
    private static final String TYPE_NEW_CHAPTER = "Добавлена новая глава";
    private static final String TYPE_CHAPTER_APPROVED = "Ваша глава прошла модерацию";
    private static final String TYPE_CHAPTER_REJECTED = "Ваша глава не прошла модерацию";
    private static final String TYPE_ADMIN_MESSAGE = "Уведомление от администрации";
    private static final String TYPE_COMIC_REMOVED_FROM_COLLECTION = "Тайтл из коллекции был удалён";
    private static final String TYPE_USER_CHAPTER_DELETED = "Ваша глава была удалена";
    private static final String TYPE_UPLOAD_RIGHTS_REVOKED = "Право на добавление глав ограничено";
    private static final String TYPE_COMPLAINT_REVIEWED = "Ваша жалоба рассмотрена";

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final ComicNotificationSubscriptionRepository subscriptionRepository;
    private final ComicRepository comicRepository;
    private final UserRepository userRepository;

    @Override
    public ModelAndView getNotificationsPage(Integer userId,
                                             String tab,
                                             int page,
                                             String q,
                                             String sortField,
                                             String sortDirection) {
        String actualTab = TAB_SUBSCRIPTIONS.equalsIgnoreCase(tab) ? TAB_SUBSCRIPTIONS : TAB_FEED;
        int safePage = Math.max(page, 0);
        String actualQuery = q == null ? "" : q.trim();
        String actualSortField = SORT_TYPE.equalsIgnoreCase(sortField) ? SORT_TYPE : SORT_CREATED_AT;
        String actualSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";

        ModelAndView mv = new ModelAndView("notifications/notifications-page");
        mv.addObject("tab", actualTab);
        mv.addObject("q", actualQuery);
        mv.addObject("sortField", actualSortField);
        mv.addObject("sortDirection", actualSortDirection);

        if (TAB_SUBSCRIPTIONS.equals(actualTab)) {
            Pageable pageable = PageRequest.of(safePage, Values.NOTIFICATION_SUBSCRIPTIONS_PAGE_SIZE);
            Page<ComicNotificationSubscription> pageData =
                    subscriptionRepository.findPageByUserIdAndQuery(userId, actualQuery, pageable);

            List<NotificationSubscriptionItem> items = pageData.getContent().stream()
                    .map(this::toSubscriptionItem)
                    .toList();

            mv.addObject("subscriptionItems", items);
            applyPagination(mv, pageData);
        } else {
            Pageable pageable = PageRequest.of(
                    safePage,
                    Values.NOTIFICATIONS_PAGE_SIZE,
                    buildNotificationSort(actualSortField, actualSortDirection)
            );

            Page<Notification> pageData = notificationRepository.findByUser_Id(userId, pageable);

            List<NotificationFeedItem> items = pageData.getContent().stream()
                    .map(this::toFeedItem)
                    .toList();

            mv.addObject("feedItems", items);
            applyPagination(mv, pageData);

            notificationRepository.markAllAsReadByUserId(userId);
        }

        long notificationCount = getNotificationCount(userId);
        mv.addObject("notificationCount", notificationCount);
        mv.addObject("hasNotifications", notificationCount > 0);

        return mv;
    }

    @Override
    @Transactional(readOnly = true)
    public long getNotificationCount(Integer userId) {
        return notificationRepository.countByUser_Id(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isComicSubscribed(Integer userId, Integer comicId) {
        return subscriptionRepository.existsByUser_IdAndComic_Id(userId, comicId);
    }

    @Override
    public NotificationToggleResult toggleComicSubscription(Integer userId, Integer comicId) {
        comicRepository.findById(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден"));

        boolean subscribed;

        if (subscriptionRepository.existsByUser_IdAndComic_Id(userId, comicId)) {
            subscriptionRepository.deleteByUser_IdAndComic_Id(userId, comicId);
            subscribed = false;
        } else {
            subscriptionRepository.save(
                    ComicNotificationSubscription.builder()
                            .user(userRepository.getReferenceById(userId))
                            .comic(comicRepository.getReferenceById(comicId))
                            .createdAt(LocalDateTime.now())
                            .build()
            );
            subscribed = true;
        }

        return new NotificationToggleResult(comicId, subscribed);
    }

    @Override
    public boolean deleteNotification(Integer userId, Integer notificationId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void notifyNewTranslationForSubscribers(Translation translation) {
        if (translation == null || translation.getChapter() == null || translation.getChapter().getComic() == null) {
            return;
        }

        Comic comic = translation.getChapter().getComic();

        subscriptionRepository.findByComic_Id(comic.getId()).forEach(subscription -> createNotification(
                subscription.getUser().getId(),
                TYPE_NEW_TRANSLATION,
                comic,
                translation.getChapter(),
                translation,
                translation.getUser() != null ? translation.getUser().getId() : null,
                translation.getId() != null ? "/read/" + translation.getId() : null,
                translation.getId() != null,
                safeMessage(translation.getTitle())
        ));
    }

    @Override
    public void notifyNewChapterForSubscribers(Chapter chapter) {
        if (chapter == null || chapter.getComic() == null) {
            return;
        }

        Comic comic = chapter.getComic();

        subscriptionRepository.findByComic_Id(comic.getId()).forEach(subscription -> createNotification(
                subscription.getUser().getId(),
                TYPE_NEW_CHAPTER,
                comic,
                chapter,
                null,
                null,
                "/comics/" + comic.getId() + "?tab=chapters",
                true,
                "В тайтле появилась новая глава."
        ));
    }

    @Override
    public void notifyChapterApproved(Integer userId, Translation translation) {
        if (userId == null || translation == null) {
            return;
        }

        Comic comic = translation.getChapter() != null ? translation.getChapter().getComic() : null;

        createNotification(
                userId,
                TYPE_CHAPTER_APPROVED,
                comic,
                translation.getChapter(),
                translation,
                null,
                translation.getId() != null
                        ? "/read/" + translation.getId()
                        : (comic != null ? "/comics/" + comic.getId() + "?tab=chapters" : null),
                translation.getId() != null || comic != null,
                "Ваша глава успешно прошла модерацию."
        );
    }

    @Override
    public void notifyChapterRejected(Integer userId, Translation translation, String message) {
        if (userId == null || translation == null) {
            return;
        }

        Comic comic = translation.getChapter() != null ? translation.getChapter().getComic() : null;

        createNotification(
                userId,
                TYPE_CHAPTER_REJECTED,
                comic,
                translation.getChapter(),
                translation,
                null,
                comic != null ? "/comics/" + comic.getId() + "?tab=chapters" : null,
                comic != null,
                hasText(message) ? message : "Глава не прошла модерацию."
        );
    }

    @Override
    public void notifyAdminMessage(Integer userId, String message) {
        if (userId == null) {
            return;
        }

        createNotification(
                userId,
                TYPE_ADMIN_MESSAGE,
                null,
                null,
                null,
                null,
                null,
                false,
                hasText(message) ? message : "У вас новое уведомление от администрации."
        );
    }

    @Override
    public void notifyComicRemovedFromCollections(Integer userId, String comicTitle) {
        if (userId == null) {
            return;
        }

        createNotification(
                userId,
                TYPE_COMIC_REMOVED_FROM_COLLECTION,
                null,
                null,
                null,
                null,
                null,
                false,
                hasText(comicTitle)
                        ? "Тайтл «" + comicTitle + "» был удалён и исчез из ваших коллекций."
                        : "Один из тайтлов из вашей коллекции был удалён."
        );
    }

    @Override
    public void notifyUserChapterDeleted(Integer userId,
                                         Integer comicId,
                                         String comicTitle,
                                         Integer chapterNumber,
                                         String languageName,
                                         String message) {
        if (userId == null) {
            return;
        }

        Comic comic = comicId != null ? comicRepository.findById(comicId).orElse(null) : null;
        Notification notification = baseNotification(userId, TYPE_USER_CHAPTER_DELETED);

        notification.setComic(comic);
        notification.setComicTitleSnapshot(hasText(comicTitle) ? comicTitle : (comic != null ? comic.getTitle() : null));
        notification.setChapterNumberSnapshot(chapterNumber);
        notification.setLanguageNameSnapshot(languageName);
        notification.setIsClickable(false);
        notification.setLinkPath(null);
        notification.setMessage(hasText(message) ? message : "Ваша глава была удалена.");

        notificationRepository.save(notification);
    }

    @Override
    public void notifyUploadRightsRevoked(Integer userId, String message) {
        if (userId == null) {
            return;
        }

        createNotification(
                userId,
                TYPE_UPLOAD_RIGHTS_REVOKED,
                null,
                null,
                null,
                null,
                null,
                false,
                hasText(message)
                        ? message
                        : "Вам временно ограничили возможность добавлять главы."
        );
    }

    @Override
    public void notifyComplaintReviewed(Integer userId,
                                        Integer comicId,
                                        String comicTitle,
                                        String message) {
        if (userId == null) {
            return;
        }

        Comic comic = comicId != null ? comicRepository.findById(comicId).orElse(null) : null;
        Notification notification = baseNotification(userId, TYPE_COMPLAINT_REVIEWED);

        notification.setComic(comic);
        notification.setComicTitleSnapshot(hasText(comicTitle) ? comicTitle : (comic != null ? comic.getTitle() : null));
        notification.setLinkPath(comic != null ? "/comics/" + comic.getId() : null);
        notification.setIsClickable(comic != null);
        notification.setMessage(hasText(message) ? message : "Ваша жалоба была рассмотрена.");

        notificationRepository.save(notification);
    }

    private Sort buildNotificationSort(String sortField, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        if (SORT_TYPE.equals(sortField)) {
            return Sort.by(direction, "type.name")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return Sort.by(direction, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private void createNotification(Integer userId,
                                    String typeCode,
                                    Comic comic,
                                    Chapter chapter,
                                    Translation translation,
                                    Integer actorUserId,
                                    String linkPath,
                                    boolean clickable,
                                    String message) {
        Notification notification = baseNotification(userId, typeCode);
        notification.setComic(comic);
        notification.setChapter(chapter);
        notification.setTranslation(translation);

        if (actorUserId != null) {
            notification.setActorUser(userRepository.getReferenceById(actorUserId));
        } else if (translation != null && translation.getUser() != null) {
            notification.setActorUser(translation.getUser());
        }

        notification.setLinkPath(linkPath);
        notification.setIsClickable(clickable && hasText(linkPath));
        notification.setComicTitleSnapshot(comic != null ? comic.getTitle() : null);
        notification.setChapterNumberSnapshot(chapter != null ? chapter.getChapterNumber() : null);
        notification.setLanguageNameSnapshot(
                translation != null && translation.getLanguage() != null ? translation.getLanguage().getName() : null
        );
        notification.setActorUsernameSnapshot(
                translation != null && translation.getUser() != null ? translation.getUser().getUsername() : null
        );
        notification.setMessage(message);

        notificationRepository.save(notification);
    }

    private Notification baseNotification(Integer userId, String typeName) {
        NotificationType type = notificationTypeRepository.findByName(typeName)
                .orElseThrow(() -> new IllegalStateException("Тип уведомления не найден: " + typeName));

        return Notification.builder()
                .user(userRepository.getReferenceById(userId))
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .isClickable(false)
                .build();
    }

    private NotificationFeedItem toFeedItem(Notification notification) {
        String typeName = notification.getType() != null && hasText(notification.getType().getName())
                ? notification.getType().getName()
                : "";

        String linkPath = Boolean.TRUE.equals(notification.getIsClickable()) && hasText(notification.getLinkPath())
                ? notification.getLinkPath()
                : null;

        return new NotificationFeedItem(
                notification.getId(),
                notification.getType() != null ? notification.getType().getName() : "Оповещение",
                resolveSubject(notification, typeName),
                resolveDetails(notification, typeName),
                notification.getComic() != null ? notification.getComic().getCover() : null,
                linkPath,
                linkPath != null,
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAtFormatted(),
                notification.getCreatedAtIso()
        );
    }

    private NotificationSubscriptionItem toSubscriptionItem(ComicNotificationSubscription subscription) {
        Comic comic = subscription.getComic();

        return new NotificationSubscriptionItem(
                comic.getId(),
                comic.getTitle(),
                comic.getOriginalTitle(),
                comic.getCover(),
                comic.getReleaseYear(),
                comic.getAvgRating()
        );
    }

    private void applyPagination(ModelAndView mv, Page<?> pageData) {
        int totalPages = pageData.getTotalPages();
        int currentPage = totalPages == 0 ? 0 : pageData.getNumber() + 1;
        int visiblePages = 5;
        int beginPage = totalPages == 0 ? 0 : Math.max(1, currentPage - 2);
        int endPage = totalPages == 0 ? 0 : Math.min(beginPage + visiblePages - 1, totalPages);

        if (totalPages > 0 && endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        mv.addObject("currentPage", currentPage);
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
        mv.addObject("showLeftDots", totalPages > 0 && beginPage > 2);
        mv.addObject("showRightDots", totalPages > 0 && endPage < totalPages - 1);
    }

    private String resolveSubject(Notification notification, String typeName) {
        String comicTitle = resolveComicTitle(notification);

        return switch (typeName) {
            case TYPE_ADMIN_MESSAGE, TYPE_UPLOAD_RIGHTS_REVOKED -> "Администрация сайта";
            case TYPE_COMPLAINT_REVIEWED -> hasText(comicTitle) ? comicTitle : "Результат по жалобе";
            case TYPE_COMIC_REMOVED_FROM_COLLECTION -> hasText(comicTitle) ? comicTitle : "Удалённый тайтл";
            case TYPE_USER_CHAPTER_DELETED -> hasText(comicTitle) ? comicTitle : "Удалённая глава";
            default -> hasText(comicTitle) ? comicTitle : "Комикс";
        };
    }


    private String resolveDetails(Notification notification, String typeName) {
        String message = safeMessage(notification.getMessage());
        Integer chapterNumber = resolveChapterNumber(notification);
        String languageName = resolveLanguageName(notification);
        String authorName = resolveActorUsername(notification);

        return switch (typeName) {
            case TYPE_NEW_TRANSLATION -> joinDetails(
                    chapterNumber != null ? "Глава " + chapterNumber : null,
                    hasText(languageName) ? "Язык: " + languageName : null,
                    hasText(authorName) ? "Автор: " + authorName : null,
                    message
            );
            case TYPE_NEW_CHAPTER -> joinDetails(
                    chapterNumber != null ? "Глава " + chapterNumber : null,
                    message
            );
            case TYPE_CHAPTER_APPROVED, TYPE_CHAPTER_REJECTED, TYPE_USER_CHAPTER_DELETED -> joinDetails(
                    chapterNumber != null ? "Глава " + chapterNumber : null,
                    hasText(languageName) ? "Язык: " + languageName : null,
                    message
            );
            default -> message;
        };
    }

    private String resolveComicTitle(Notification notification) {
        if (notification.getComic() != null && hasText(notification.getComic().getTitle())) {
            return notification.getComic().getTitle();
        }
        return notification.getComicTitleSnapshot();
    }

    private Integer resolveChapterNumber(Notification notification) {
        if (notification.getChapter() != null && notification.getChapter().getChapterNumber() != null) {
            return notification.getChapter().getChapterNumber();
        }
        if (notification.getTranslation() != null
                && notification.getTranslation().getChapter() != null
                && notification.getTranslation().getChapter().getChapterNumber() != null) {
            return notification.getTranslation().getChapter().getChapterNumber();
        }
        return notification.getChapterNumberSnapshot();
    }

    private String resolveLanguageName(Notification notification) {
        if (notification.getTranslation() != null
                && notification.getTranslation().getLanguage() != null
                && hasText(notification.getTranslation().getLanguage().getName())) {
            return notification.getTranslation().getLanguage().getName();
        }
        return notification.getLanguageNameSnapshot();
    }

    private String resolveActorUsername(Notification notification) {
        if (notification.getActorUser() != null && hasText(notification.getActorUser().getUsername())) {
            return notification.getActorUser().getUsername();
        }
        if (notification.getTranslation() != null
                && notification.getTranslation().getUser() != null
                && hasText(notification.getTranslation().getUser().getUsername())) {
            return notification.getTranslation().getUser().getUsername();
        }
        return notification.getActorUsernameSnapshot();
    }

    private String joinDetails(String... values) {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" • ");
            }

            builder.append(value);
        }

        return builder.toString();
    }

    private String safeMessage(String value) {
        return hasText(value) ? value.trim() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
