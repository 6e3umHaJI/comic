package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicNotificationSubscription;
import by.bsuir.springbootproject.entities.ComicPage;
import by.bsuir.springbootproject.entities.SavedComic;
import by.bsuir.springbootproject.entities.Translation;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.repositories.ChapterRepository;
import by.bsuir.springbootproject.repositories.ComicNotificationSubscriptionRepository;
import by.bsuir.springbootproject.repositories.ComicPageRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComplaintRepository;
import by.bsuir.springbootproject.repositories.NotificationRepository;
import by.bsuir.springbootproject.repositories.RatingRepository;
import by.bsuir.springbootproject.repositories.ReadProgressRepository;
import by.bsuir.springbootproject.repositories.SavedComicRepository;
import by.bsuir.springbootproject.repositories.TranslationRepository;
import by.bsuir.springbootproject.services.AdminComicDeletionService;
import by.bsuir.springbootproject.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import by.bsuir.springbootproject.services.UploadStorageService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminComicDeletionServiceImpl implements AdminComicDeletionService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String SCOPE_COMIC = "COMIC";
    private static final String SCOPE_TRANSLATION = "TRANSLATION";

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final TranslationRepository translationRepository;
    private final ComicPageRepository comicPageRepository;
    private final SavedComicRepository savedComicRepository;
    private final ComicNotificationSubscriptionRepository subscriptionRepository;
    private final ReadProgressRepository readProgressRepository;
    private final RatingRepository ratingRepository;
    private final NotificationRepository notificationRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final UploadStorageService uploadStorageService;


    @Override
    public void deleteComic(Integer comicId, User admin) {
        requireAdmin(admin);

        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден."));

        String comicTitle = comic.getTitle();
        String coverFileName = comic.getCover();

        List<Translation> translations = translationRepository.findAllByChapter_Comic_IdOrderByChapter_ChapterNumberAscIdAsc(comicId);
        List<Integer> translationIds = translations.stream()
                .map(Translation::getId)
                .filter(Objects::nonNull)
                .toList();

        List<ComicPage> comicPages = comicPageRepository.findByTranslation_Chapter_Comic_IdOrderByTranslation_IdAscPageNumberAsc(comicId);
        Set<String> pageFileNames = comicPages.stream()
                .map(ComicPage::getImagePath)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> collectionUserIds = savedComicRepository.findByComic_Id(comicId).stream()
                .map(SavedComic::getSection)
                .filter(Objects::nonNull)
                .map(section -> section.getUser())
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> subscribedUserIds = subscriptionRepository.findByComic_Id(comicId).stream()
                .map(ComicNotificationSubscription::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> translationOwnerIds = translations.stream()
                .map(Translation::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        collectionUserIds.forEach(userId ->
                notificationService.notifyComicRemovedFromCollections(userId, comicTitle)
        );

        subscribedUserIds.forEach(userId ->
                notificationService.notifyAdminMessage(
                        userId,
                        "Тайтл «" + comicTitle + "» был удалён. Подписка на оповещения по нему отключена."
                )
        );

        translationOwnerIds.forEach(userId ->
                notificationService.notifyAdminMessage(
                        userId,
                        "Комикс «" + comicTitle + "» был удалён. Вместе с ним были удалены ваши переводы."
                )
        );

        complaintRepository.deleteByTargetIdAndScope(comicId, SCOPE_COMIC);
        if (!translationIds.isEmpty()) {
            complaintRepository.deleteByTargetIdsAndScope(translationIds, SCOPE_TRANSLATION);
        }

        notificationRepository.detachDeletedComic(comicId);
        readProgressRepository.deleteAllByComicId(comicId);
        ratingRepository.deleteAllByComicId(comicId);
        savedComicRepository.deleteAllByComicId(comicId);
        subscriptionRepository.deleteAllByComicId(comicId);
        comicPageRepository.deleteAllByComicId(comicId);
        translationRepository.deleteAllByComicId(comicId);
        chapterRepository.deleteAllByComicId(comicId);

        comicRepository.delete(comic);
        comicRepository.flush();

        deletePageFiles(pageFileNames);
        deleteUnusedCover(coverFileName);
    }

    private void requireAdmin(User admin) {
        if (admin == null || admin.getRole() == null || !ADMIN_ROLE.equalsIgnoreCase(admin.getRole().getName())) {
            throw new IllegalStateException("Недостаточно прав.");
        }
    }

    private void deletePageFiles(Set<String> fileNames) {
        for (String fileName : fileNames) {
            uploadStorageService.deletePageIfExists(fileName);
        }
    }

    private void deleteUnusedCover(String coverFileName) {
        if (!StringUtils.hasText(coverFileName)) {
            return;
        }

        if (comicRepository.countByCover(coverFileName) > 0) {
            return;
        }

        uploadStorageService.deleteCoverIfExists(coverFileName);
    }
}
