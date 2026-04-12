package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.constants.Values;
import by.bsuir.springbootproject.dto.*;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.SavedComic;
import by.bsuir.springbootproject.entities.User;
import by.bsuir.springbootproject.entities.UserSection;
import by.bsuir.springbootproject.repositories.ComicRepository;
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

    private static final List<String> DEFAULT_SECTIONS = List.of("Читаю", "В планах", "Прочитано");

    private final UserSectionRepository userSectionRepository;
    private final SavedComicRepository savedComicRepository;
    private final ComicRepository comicRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    @Transactional(readOnly = true)
    public ModelAndView getCollectionsPage(Integer userId, Integer sectionId, int page, String viewMode) {
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
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Values.COLLECTIONS_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "addedAt")
        );

        Page<SavedComic> savedPage = savedComicRepository.findPageBySectionId(activeSection.getId(), pageable);

        int totalPages = savedPage.getTotalPages();
        int currentPage = savedPage.getNumber() + 1;
        int visiblePages = 5;
        int beginPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(beginPage + visiblePages - 1, Math.max(totalPages, 1));

        if (endPage - beginPage < visiblePages - 1) {
            beginPage = Math.max(1, endPage - visiblePages + 1);
        }

        boolean showLeftDots = beginPage > 2;
        boolean showRightDots = endPage < totalPages - 1;

        ModelAndView mv = new ModelAndView();
        mv.addObject("sections", sections);
        mv.addObject("activeSection", activeSection);
        mv.addObject("savedComics", savedPage.getContent());
        mv.addObject("viewMode", actualViewMode);
        mv.addObject("currentPage", currentPage);
        mv.addObject("totalPages", totalPages);
        mv.addObject("beginPage", beginPage);
        mv.addObject("endPage", endPage);
        mv.addObject("showLeftDots", showLeftDots);
        mv.addObject("showRightDots", showRightDots);
        mv.addObject("hasSavedComics", !savedPage.getContent().isEmpty());
        return mv;
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

        Comic comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден"));

        for (Integer currentId : currentIds) {
            if (!targetIds.contains(currentId)) {
                savedComicRepository.deleteBySectionIdAndComicId(currentId, comicId);
            }
        }

        for (Integer targetId : targetIds) {
            if (!currentIds.contains(targetId)) {
                UserSection section = userSectionRepository.findByIdAndUserId(targetId, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

                savedComicRepository.save(
                        SavedComic.builder()
                                .section(section)
                                .comic(comic)
                                .addedAt(LocalDateTime.now())
                                .build()
                );
            }
        }
    }

    @Override
    public void createSection(Integer userId, CollectionCreateForm form) {
        if (form == null || form.getName() == null) {
            throw new IllegalArgumentException("Введите название категории");
        }

        long currentCount = userSectionRepository.countByUserId(userId);
        if (currentCount >= Values.MAX_USER_COLLECTIONS) {
            throw new IllegalStateException("Максимальное количество категорий " + Values.MAX_USER_COLLECTIONS);
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
            if (form.getTargetSectionId() == null) {
                throw new IllegalArgumentException("Выберите категорию, в которую перенести тайтлы");
            }

            UserSection target = userSectionRepository.findByIdAndUserId(form.getTargetSectionId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория переноса не найдена"));

            if (source.getId().equals(target.getId())) {
                throw new IllegalArgumentException("Нужно выбрать другую категорию");
            }

            UserSection targetRef = entityManager.getReference(UserSection.class, target.getId());

            for (SavedComic item : sourceItems) {
                Integer comicId = item.getComic().getId();

                if (!savedComicRepository.existsBySectionIdAndComicId(targetRef.getId(), comicId)) {
                    SavedComic moved = new SavedComic();
                    moved.setSection(targetRef);
                    moved.setComic(entityManager.getReference(by.bsuir.springbootproject.entities.Comic.class, comicId));
                    moved.setAddedAt(LocalDateTime.now());

                    savedComicRepository.save(moved);
                }
            }


            List<Integer> sourceComicIds = sourceItems.stream()
                    .map(item -> item.getComic().getId())
                    .toList();

            savedComicRepository.deleteBySectionIdAndComicIds(source.getId(), sourceComicIds);
        }

        userSectionRepository.delete(source);
        userSectionRepository.flush();
    }

    @Override
    public void moveComics(Integer userId, CollectionMoveForm form) {
        if (form == null || form.getComicIds() == null || form.getComicIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один тайтл");
        }

        UserSection from = userSectionRepository.findByIdAndUserId(form.getFromSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Исходная категория не найдена"));

        UserSection to = userSectionRepository.findByIdAndUserId(form.getToSectionId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Целевая категория не найдена"));

        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Выберите другую категорию");
        }

        List<Integer> selectedComicIds = form.getComicIds();

        List<SavedComic> fromItems = savedComicRepository.findAllBySectionId(from.getId()).stream()
                .filter(sc -> selectedComicIds.contains(sc.getComic().getId()))
                .toList();

        if (fromItems.isEmpty()) {
            throw new IllegalArgumentException("Нет выбранных тайтлов для переноса");
        }

        UserSection toRef = entityManager.getReference(UserSection.class, to.getId());

        for (SavedComic item : fromItems) {
            Integer comicId = item.getComic().getId();

            if (!savedComicRepository.existsBySectionIdAndComicId(toRef.getId(), comicId)) {
                SavedComic moved = new SavedComic();
                moved.setSection(toRef);
                moved.setComic(entityManager.getReference(by.bsuir.springbootproject.entities.Comic.class, comicId));
                moved.setAddedAt(LocalDateTime.now());

                savedComicRepository.save(moved);
            }
        }


        savedComicRepository.deleteBySectionIdAndComicIds(
                from.getId(),
                fromItems.stream().map(item -> item.getComic().getId()).toList()
        );
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


    @Override
    public void ensureDefaultSections(User user) {
        List<UserSection> existing = userSectionRepository.findByUserIdOrderByIsDefaultDescNameAsc(user.getId());
        Set<String> names = existing.stream()
                .map(UserSection::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String name : DEFAULT_SECTIONS) {
            if (!names.contains(name.toLowerCase())) {
                userSectionRepository.save(
                        UserSection.builder()
                                .user(user)
                                .name(name)
                                .isDefault(true)
                                .build()
                );
            }
        }
    }
}
