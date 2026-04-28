package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.dto.*;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.SavedComic;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserSection;
import by.bsuir.springbootproject.repositories.SavedComicRepository;
import by.bsuir.springbootproject.repositories.UserSectionRepository;
import by.bsuir.springbootproject.services.CollectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionServiceImpl implements CollectionService {

    private final UserSectionRepository userSectionRepository;
    private final SavedComicRepository savedComicRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    @Transactional(readOnly = true)
    public ModelAndView getCollectionsPage(Integer userId,
                                           Integer sectionId,
                                           int page,
                                           String viewMode,
                                           String q,
                                           String sortField,
                                           String sortDirection) {
        List<CollectionSidebarItem> sections = userSectionRepository.findAllWithCountsByUserId(userId)
                .stream()
                .map(row -> {
                    UserSection section = (UserSection) row[0];
                    long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
                    return new CollectionSidebarItem(section.getId(), section.getName(), section.getIsDefault(), count);
                })
                .toList();

        if (sections.isEmpty()) {
            throw new IllegalStateException("Разделы не найдены.");
        }

        Set<Integer> existingIds = sections.stream()
                .map(CollectionSidebarItem::getId)
                .collect(Collectors.toSet());

        Integer activeSectionId = (sectionId != null && existingIds.contains(sectionId))
                ? sectionId
                : sections.getFirst().getId();

        UserSection activeSection = userSectionRepository.findByIdAndUserId(activeSectionId, userId)
                .orElseGet(() -> userSectionRepository.findByIdAndUserId(sections.getFirst().getId(), userId)
                        .orElseThrow(() -> new IllegalArgumentException("Категория не найдена")));

        String actualViewMode = "list".equalsIgnoreCase(viewMode) ? "list" : "card";
        String actualQuery = q == null ? "" : q.trim();
        String actualSortField = normalizeCollectionsSortField(sortField);
        String actualSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";

        List<SavedComic> allSavedComics = savedComicRepository.findAllBySectionId(activeSection.getId());

        List<SavedComic> filteredComics = allSavedComics.stream()
                .filter(savedComic -> matchesCollectionsQuery(savedComic.getComic(), actualQuery))
                .sorted(buildCollectionsComparator(actualSortField, actualSortDirection))
                .toList();

        int totalItems = filteredComics.size();
        int pageSize = Values.COLLECTIONS_PAGE_SIZE;
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);

        int safePage = totalPages == 0
                ? 0
                : Math.clamp(page, 0, totalPages - 1);

        int fromIndex = totalPages == 0 ? 0 : safePage * pageSize;
        int toIndex = totalPages == 0 ? 0 : Math.min(fromIndex + pageSize, totalItems);

        List<SavedComic> pageItems = totalPages == 0
                ? List.of()
                : filteredComics.subList(fromIndex, toIndex);

        int currentPage = totalPages == 0 ? 0 : safePage + 1;
        int visiblePages = 5;
        int beginPage = totalPages == 0 ? 0 : Math.max(1, currentPage - 2);
        int endPage = totalPages == 0 ? 0 : Math.min(beginPage + visiblePages - 1, totalPages);

        if (totalPages > 0 && endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        boolean showLeftDots = totalPages > 0 && beginPage > 2;
        boolean showRightDots = totalPages > 0 && endPage < totalPages - 1;

        ModelAndView mv = new ModelAndView();
        mv.addObject("sections", sections);
        mv.addObject("activeSection", activeSection);
        mv.addObject("savedComics", pageItems);
        mv.addObject("viewMode", actualViewMode);
        mv.addObject("q", actualQuery);
        mv.addObject("sortField", actualSortField);
        mv.addObject("sortDirection", actualSortDirection);
        mv.addObject("currentPage", currentPage);
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
        mv.addObject("showLeftDots", showLeftDots);
        mv.addObject("showRightDots", showRightDots);
        mv.addObject("hasSavedComics", !allSavedComics.isEmpty());
        mv.addObject("hasVisibleSavedComics", !pageItems.isEmpty());
        return mv;
    }

    private String normalizeCollectionsSortField(String sortField) {
        if (sortField == null) {
            return "addedAt";
        }

        return switch (sortField) {
            case "addedAt", "popularityScore", "avgRating", "title", "releaseYear", "createdAt", "updatedAt" -> sortField;
            default -> "addedAt";
        };
    }

    private boolean matchesCollectionsQuery(Comic comic, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);

        return containsIgnoreCase(comic.getTitle(), normalizedQuery)
                || containsIgnoreCase(comic.getOriginalTitle(), normalizedQuery);
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<SavedComic> buildCollectionsComparator(String sortField, String sortDirection) {
        Comparator<SavedComic> comparator = switch (sortField) {
            case "title" -> Comparator.comparing(
                    saved -> normalizeString(saved.getComic().getTitle())
            );
            case "releaseYear" -> Comparator.comparing(
                    saved -> saved.getComic().getReleaseYear(),
                    Comparator.nullsLast(Integer::compareTo)
            );
            case "createdAt" -> Comparator.comparing(
                    saved -> saved.getComic().getCreatedAt(),
                    Comparator.nullsLast(LocalDateTime::compareTo)
            );
            case "updatedAt" -> Comparator.comparing(
                    saved -> saved.getComic().getUpdatedAt(),
                    Comparator.nullsLast(LocalDateTime::compareTo)
            );
            case "popularityScore" -> Comparator.comparingLong(
                    saved -> saved.getComic().getPopularityScore() == null ? Long.MIN_VALUE : saved.getComic().getPopularityScore()
            );
            case "avgRating" -> Comparator.comparingDouble(
                    saved -> saved.getComic().getAvgRating() == null ? Double.NEGATIVE_INFINITY : saved.getComic().getAvgRating()
            );
            default -> Comparator.comparing(
                    SavedComic::getAddedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
            );
        };

        comparator = comparator.thenComparing(
                saved -> saved.getComic().getId(),
                Comparator.nullsLast(Integer::compareTo)
        );

        return "asc".equalsIgnoreCase(sortDirection) ? comparator : comparator.reversed();
    }

    private String normalizeString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }



    @Override
    @Transactional(readOnly = true)
    public ModelAndView getComicModal(Integer userId, Integer comicId) {
        List<CollectionSidebarItem> sections = userSectionRepository.findAllWithCountsByUserId(userId)
                .stream()
                .map(row -> {
                    UserSection section = (UserSection) row[0];
                    long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
                    return new CollectionSidebarItem(section.getId(), section.getName(), section.getIsDefault(), count);
                })
                .toList();

        List<Integer> selectedSectionIds = savedComicRepository.findSectionIdsByUserIdAndComicId(userId, comicId);

        ModelAndView mv = new ModelAndView();
        mv.addObject("sections", sections);
        mv.addObject("selectedSectionIds", selectedSectionIds);
        mv.addObject("comicId", comicId);
        mv.addObject("inCollections", !selectedSectionIds.isEmpty());
        return mv;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isComicInCollections(Integer userId, Integer comicId) {
        return savedComicRepository.existsBySectionUserIdAndComicId(userId, comicId);
    }

    @Override
    public void syncComicCollections(Integer userId, Integer comicId, List<Integer> sectionIds) {
        List<UserSection> allSections = userSectionRepository.findByUserIdOrderByIsDefaultDescNameAsc(userId);

        Set<Integer> allowedIds = allSections.stream()
                .map(UserSection::getId)
                .collect(Collectors.toSet());

        Set<Integer> targetIds = (sectionIds == null ? Collections.<Integer>emptySet() : new HashSet<>(sectionIds))
                .stream()
                .filter(allowedIds::contains)
                .collect(Collectors.toSet());

        List<Integer> currentIds = savedComicRepository.findSectionIdsByUserIdAndComicId(userId, comicId);

        for (Integer currentId : currentIds) {
            if (!targetIds.contains(currentId)) {
                savedComicRepository.deleteBySectionIdAndComicId(currentId, comicId);
            }
        }

        for (Integer targetId : targetIds) {
            if (!currentIds.contains(targetId)) {
                savedComicRepository.insertSavedComic(
                        targetId,
                        comicId,
                        LocalDateTime.now()
                );
            }
        }
    }


    @Override
    public void createSection(Integer userId, CollectionCreateForm form) {
        if (form == null || form.getName() == null) {
            throw new IllegalArgumentException("Введите название категории");
        }

        long currentCount = userSectionRepository.countByUserIdAndIsDefaultFalse(userId);

        if (currentCount >= Values.MAX_USER_COLLECTIONS) {
            throw new IllegalStateException("Максимальное количество пользовательских категорий " + Values.MAX_USER_COLLECTIONS);
        }


        String name = form.getName().trim();
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Название должно быть от 2 до 100 символов");
        }

        if (userSectionRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }

        userSectionRepository.save(
                UserSection.builder()
                        .user(User.builder().id(userId).build())
                        .name(name)
                        .isDefault(false)
                        .build()
        );
    }

    @Override
    public void renameSection(Integer userId, CollectionRenameForm form) {
        if (form == null || form.getSectionId() == null) {
            throw new IllegalArgumentException("Категория не выбрана");
        }

        UserSection section = userSectionRepository.findByIdAndUserId(form.getSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        if (Boolean.TRUE.equals(section.getIsDefault())) {
            throw new IllegalStateException("Стандартные категории нельзя переименовывать");
        }

        String newName = form.getName() == null ? "" : form.getName().trim();
        if (newName.length() < 2 || newName.length() > 100) {
            throw new IllegalArgumentException("Название должно быть от 2 до 100 символов");
        }

        if (section.getName().equalsIgnoreCase(newName)) {
            throw new IllegalArgumentException("Новое название совпадает с текущим");
        }

        if (userSectionRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, newName, section.getId())) {
            throw new IllegalArgumentException("Категория с таким названием уже существует");
        }

        section.setName(newName);
    }

    @Override
    public void deleteSection(Integer userId, CollectionDeleteForm form) {
        if (form == null || form.getSectionId() == null) {
            throw new IllegalArgumentException("Категория не выбрана");
        }

        UserSection source = userSectionRepository.findByIdAndUserId(form.getSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        if (Boolean.TRUE.equals(source.getIsDefault())) {
            throw new IllegalStateException("Стандартные категории нельзя удалять");
        }

        List<SavedComic> sourceItems = savedComicRepository.findAllBySectionId(source.getId());

        if (!sourceItems.isEmpty()) {
            boolean deleteComics = form.getDeleteComics() == null || form.getDeleteComics();

            if (!deleteComics) {
                List<Integer> targetIds = form.getTargetSectionIds();

                if (targetIds == null || targetIds.isEmpty()) {
                    throw new IllegalArgumentException("Выберите хотя бы одну категорию переноса");
                }

                List<UserSection> targets = targetIds.stream()
                        .distinct()
                        .map(id -> userSectionRepository.findByIdAndUserId(id, userId)
                                .orElseThrow(() -> new IllegalArgumentException("Категория переноса не найдена")))
                        .filter(target -> !source.getId().equals(target.getId()))
                        .toList();

                if (targets.isEmpty()) {
                    throw new IllegalArgumentException("Выберите хотя бы одну другую категорию");
                }

                for (SavedComic item : sourceItems) {
                    Integer comicId = item.getComic().getId();

                    for (UserSection target : targets) {
                        if (!savedComicRepository.existsBySectionIdAndComicId(target.getId(), comicId)) {
                            savedComicRepository.insertSavedComic(
                                    target.getId(),
                                    comicId,
                                    LocalDateTime.now()
                            );
                        }
                    }
                }
            }

            List<Integer> sourceComicIds = sourceItems.stream()
                    .map(item -> item.getComic().getId())
                    .toList();

            savedComicRepository.deleteBySectionIdAndComicIds(source.getId(), sourceComicIds);
            entityManager.flush();
            entityManager.clear();
        }

        UserSection sourceToDelete = userSectionRepository.findByIdAndUserId(form.getSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        userSectionRepository.delete(sourceToDelete);
        userSectionRepository.flush();
    }



    @Override
    public void moveComics(Integer userId, CollectionMoveForm form) {
        if (form == null || form.getComicIds() == null || form.getComicIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один тайтл");
        }

        UserSection from = userSectionRepository.findByIdAndUserId(form.getFromSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Исходная категория не найдена"));

        List<Integer> targetIds = form.getToSectionIds();
        if (targetIds == null || targetIds.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы одну целевую категорию");
        }

        List<UserSection> targets = targetIds.stream()
                .distinct()
                .map(id -> userSectionRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Целевая категория не найдена")))
                .filter(target -> !from.getId().equals(target.getId()))
                .toList();

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы одну другую категорию");
        }

        List<Integer> selectedComicIds = form.getComicIds();

        List<SavedComic> fromItems = savedComicRepository.findAllBySectionId(from.getId()).stream()
                .filter(sc -> selectedComicIds.contains(sc.getComic().getId()))
                .toList();

        if (fromItems.isEmpty()) {
            throw new IllegalArgumentException("Нет выбранных тайтлов для переноса");
        }

        for (SavedComic item : fromItems) {
            Integer comicId = item.getComic().getId();

            for (UserSection target : targets) {
                if (!savedComicRepository.existsBySectionIdAndComicId(target.getId(), comicId)) {
                    savedComicRepository.insertSavedComic(
                            target.getId(),
                            comicId,
                            LocalDateTime.now()
                    );
                }
            }
        }

        savedComicRepository.deleteBySectionIdAndComicIds(
                from.getId(),
                fromItems.stream().map(item -> item.getComic().getId()).toList()
        );

        entityManager.flush();
        entityManager.clear();
    }



    @Override
    public void removeComics(Integer userId, CollectionRemoveForm form) {
        if (form == null || form.getSectionId() == null) {
            throw new IllegalArgumentException("Категория не выбрана");
        }

        if (form.getComicIds() == null || form.getComicIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один тайтл");
        }

        userSectionRepository.findByIdAndUserId(form.getSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        savedComicRepository.deleteBySectionIdAndComicIds(form.getSectionId(), form.getComicIds());
    }
}
