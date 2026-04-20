package by.bsuir.springbootproject.services.implementation;

import by.bsuir.springbootproject.dto.AdminComicForm;
import by.bsuir.springbootproject.dto.AdminComicRelationItem;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.ComicRelation;
import by.bsuir.springbootproject.entities.Genre;
import by.bsuir.springbootproject.entities.RelationType;
import by.bsuir.springbootproject.entities.Tag;
import by.bsuir.springbootproject.repositories.AgeRatingRepository;
import by.bsuir.springbootproject.repositories.ComicRepository;
import by.bsuir.springbootproject.repositories.ComicStatusRepository;
import by.bsuir.springbootproject.repositories.ComicTypeRepository;
import by.bsuir.springbootproject.repositories.GenreRepository;
import by.bsuir.springbootproject.repositories.RelationTypeRepository;
import by.bsuir.springbootproject.repositories.TagRepository;
import by.bsuir.springbootproject.services.AdminComicManageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminComicManageServiceImpl implements AdminComicManageService {

    private static final String VIEW_NAME = "admin/comic-form";
    private static final long MAX_COVER_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_COVER_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ComicRepository comicRepository;
    private final ComicTypeRepository comicTypeRepository;
    private final AgeRatingRepository ageRatingRepository;
    private final ComicStatusRepository comicStatusRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final RelationTypeRepository relationTypeRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public ModelAndView getCreatePage(AdminComicForm form, String errorMessage) {
        AdminComicForm actualForm = normalizeFormForView(form, null);

        ModelAndView mv = new ModelAndView(VIEW_NAME);
        mv.addObject("form", actualForm);
        mv.addObject("isEdit", false);
        mv.addObject("pageTitle", "Добавить комикс");
        mv.addObject("errorMessage", errorMessage);
        fillReferenceData(mv);
        return mv;
    }

    @Override
    public ModelAndView getEditPage(Integer comicId, AdminComicForm form, String errorMessage) {
        Comic comic = comicRepository.findByIdForAdminEdit(comicId)
                .orElseThrow(() -> new IllegalArgumentException("Комикс не найден."));

        AdminComicForm actualForm = normalizeFormForView(form, comic);

        ModelAndView mv = new ModelAndView(VIEW_NAME);
        mv.addObject("form", actualForm);
        mv.addObject("isEdit", true);
        mv.addObject("pageTitle", "Редактировать комикс");
        mv.addObject("errorMessage", errorMessage);
        fillReferenceData(mv);
        return mv;
    }

    @Override
    @Transactional
    public Integer saveComic(AdminComicForm form, MultipartFile coverFile) {
        boolean isEdit = form.getComicId() != null;

        Comic comic = isEdit
                ? comicRepository.findByIdForAdminEdit(form.getComicId())
                  .orElseThrow(() -> new IllegalArgumentException("Комикс не найден."))
                : Comic.builder().build();

        ensureCollectionsInitialized(comic);

        String title = requiredTrimmed(form.getTitle(), "Укажите название комикса.", 255);
        String originalTitle = optionalTrimmed(form.getOriginalTitle(), 255);
        Integer releaseYear = parseReleaseYear(form.getReleaseYear());
        String shortDescription = requiredTrimmed(form.getShortDescription(), "Укажите краткое описание.", 500);
        String fullDescription = requiredTrimmed(form.getFullDescription(), "Укажите описание.", 2000);

        comic.setTitle(title);
        comic.setOriginalTitle(originalTitle);
        comic.setReleaseYear(releaseYear);
        comic.setShortDescription(shortDescription);
        comic.setFullDescription(fullDescription);
        comic.setType(comicTypeRepository.findById(form.getTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Выберите тип комикса.")));
        comic.setComicStatus(comicStatusRepository.findById(form.getComicStatusId())
                .orElseThrow(() -> new IllegalArgumentException("Выберите статус комикса.")));
        comic.setAgeRating(form.getAgeRatingId() != null
                ? ageRatingRepository.findById(form.getAgeRatingId()).orElse(null)
                : null);

        if (coverFile != null && !coverFile.isEmpty()) {
            comic.setCover(storeCover(coverFile));
        } else if (!isEdit && (comic.getCover() == null || comic.getCover().isBlank())) {
            throw new IllegalArgumentException("Загрузите обложку комикса.");
        }

        if (!isEdit) {
            comic.setCreatedAt(LocalDateTime.now());
            comic.setUpdatedAt(LocalDateTime.now());
            comic.setPopularityScore(0L);
            comic = comicRepository.save(comic);
            ensureCollectionsInitialized(comic);
        }

        applyGenres(comic, form);
        applyTags(comic, form);
        applyRelationTypeOperations(form.getRelationTypeOperationsJson());
        applyRelations(comic, form.getRelationsJson());

        comicRepository.save(comic);
        return comic.getId();
    }

    @Override
    public List<Map<String, Object>> searchComics(String q, Integer excludeComicId) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) {
            return List.of();
        }

        return comicRepository.findQuickSearchResults(query, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .filter(comic -> excludeComicId == null || !comic.getId().equals(excludeComicId))
                .map(comic -> Map.<String, Object>of(
                        "id", comic.getId(),
                        "title", comic.getTitle(),
                        "originalTitle", comic.getOriginalTitle() != null ? comic.getOriginalTitle() : "",
                        "cover", comic.getCover() != null ? comic.getCover() : ""
                ))
                .toList();
    }

    private void fillReferenceData(ModelAndView mv) {
        mv.addObject("comicTypes", comicTypeRepository.findAllByOrderByNameAsc());
        mv.addObject("ageRatings", ageRatingRepository.findAllByOrderByIdAsc());
        mv.addObject("comicStatuses", comicStatusRepository.findAllByOrderByNameAsc());
        mv.addObject("genres", genreRepository.findAllByOrderByNameAsc());
        mv.addObject("tags", tagRepository.findAllByOrderByNameAsc());
        mv.addObject("relationTypes", relationTypeRepository.findAllByOrderByNameAsc());
    }

    private AdminComicForm normalizeFormForView(AdminComicForm form, Comic comic) {
        AdminComicForm actualForm = form != null ? form : mapComicToForm(comic);

        if (actualForm == null) {
            actualForm = AdminComicForm.builder().build();
        }

        if (actualForm.getGenreIds() == null) {
            actualForm.setGenreIds(new ArrayList<>());
        }
        if (actualForm.getTagIds() == null) {
            actualForm.setTagIds(new ArrayList<>());
        }
        if (actualForm.getRelationItems() == null) {
            actualForm.setRelationItems(new ArrayList<>());
        }
        if (actualForm.getGenreOperationsJson() == null || actualForm.getGenreOperationsJson().isBlank()) {
            actualForm.setGenreOperationsJson("[]");
        }
        if (actualForm.getTagOperationsJson() == null || actualForm.getTagOperationsJson().isBlank()) {
            actualForm.setTagOperationsJson("[]");
        }
        if (actualForm.getRelationTypeOperationsJson() == null || actualForm.getRelationTypeOperationsJson().isBlank()) {
            actualForm.setRelationTypeOperationsJson("[]");
        }
        if (actualForm.getRelationsJson() == null || actualForm.getRelationsJson().isBlank()) {
            actualForm.setRelationsJson("[]");
        }

        if ((actualForm.getCurrentCover() == null || actualForm.getCurrentCover().isBlank()) && comic != null) {
            actualForm.setCurrentCover(comic.getCover());
        }

        if ((actualForm.getRelationItems() == null || actualForm.getRelationItems().isEmpty())) {
            if (actualForm.getRelationsJson() != null && !actualForm.getRelationsJson().isBlank() && !"[]".equals(actualForm.getRelationsJson())) {
                actualForm.setRelationItems(parseRelationItemsForView(actualForm.getRelationsJson()));
            } else if (comic != null && comic.getRelatedComics() != null) {
                actualForm.setRelationItems(
                        comic.getRelatedComics().stream()
                                .map(relation -> new AdminComicRelationItem(
                                        relation.getRelatedComic() != null ? relation.getRelatedComic().getId() : null,
                                        relation.getRelatedComic() != null ? relation.getRelatedComic().getTitle() : "",
                                        relation.getRelationType() != null ? relation.getRelationType().getName() : ""
                                ))
                                .toList()
                );
            }
        }

        return actualForm;
    }

    private AdminComicForm mapComicToForm(Comic comic) {
        if (comic == null) {
            return AdminComicForm.builder().build();
        }

        ensureCollectionsInitialized(comic);

        return AdminComicForm.builder()
                .comicId(comic.getId())
                .currentCover(comic.getCover())
                .title(comic.getTitle())
                .originalTitle(comic.getOriginalTitle())
                .releaseYear(comic.getReleaseYear() != null ? String.valueOf(comic.getReleaseYear()) : "")
                .shortDescription(comic.getShortDescription())
                .fullDescription(comic.getFullDescription())
                .typeId(comic.getType() != null ? comic.getType().getId() : null)
                .ageRatingId(comic.getAgeRating() != null ? comic.getAgeRating().getId() : null)
                .comicStatusId(comic.getComicStatus() != null ? comic.getComicStatus().getId() : null)
                .genreIds(comic.getGenres().stream().map(Genre::getId).toList())
                .tagIds(comic.getTags().stream().map(Tag::getId).toList())
                .relationItems(
                        comic.getRelatedComics().stream()
                                .map(relation -> new AdminComicRelationItem(
                                        relation.getRelatedComic() != null ? relation.getRelatedComic().getId() : null,
                                        relation.getRelatedComic() != null ? relation.getRelatedComic().getTitle() : "",
                                        relation.getRelationType() != null ? relation.getRelationType().getName() : ""
                                ))
                                .toList()
                )
                .genreOperationsJson("[]")
                .tagOperationsJson("[]")
                .relationTypeOperationsJson("[]")
                .relationsJson("[]")
                .build();
    }

    private void applyGenres(Comic comic, AdminComicForm form) {
        ensureCollectionsInitialized(comic);

        Set<Integer> selectedIds = new LinkedHashSet<>();
        if (form.getGenreIds() != null) {
            selectedIds.addAll(form.getGenreIds());
        }

        for (LookupOperation op : parseLookupOperations(form.getGenreOperationsJson())) {
            String name = optionalTrimmed(op.getName(), 100);

            if (op.getId() != null) {
                Genre genre = genreRepository.findById(op.getId()).orElse(null);
                if (genre == null) {
                    continue;
                }

                if (Boolean.TRUE.equals(op.getDelete())) {
                    selectedIds.remove(op.getId());
                    jdbcTemplate.update("delete from comic_genres where genre_id = ?", op.getId());
                    genreRepository.delete(genre);
                    continue;
                }

                if (!name.isBlank()) {
                    genre.setName(name);
                    genreRepository.save(genre);
                }

                if (Boolean.TRUE.equals(op.getSelected())) {
                    selectedIds.add(genre.getId());
                }
                continue;
            }

            if (Boolean.TRUE.equals(op.getDelete()) || name.isBlank()) {
                continue;
            }

            Genre genre = genreRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> genreRepository.save(Genre.builder().name(name).build()));

            if (Boolean.TRUE.equals(op.getSelected())) {
                selectedIds.add(genre.getId());
            }
        }

        comic.getGenres().clear();
        comic.getGenres().addAll(new LinkedHashSet<>(genreRepository.findAllById(selectedIds)));
    }

    private void applyTags(Comic comic, AdminComicForm form) {
        ensureCollectionsInitialized(comic);

        Set<Integer> selectedIds = new LinkedHashSet<>();
        if (form.getTagIds() != null) {
            selectedIds.addAll(form.getTagIds());
        }

        for (LookupOperation op : parseLookupOperations(form.getTagOperationsJson())) {
            String name = optionalTrimmed(op.getName(), 100);

            if (op.getId() != null) {
                Tag tag = tagRepository.findById(op.getId()).orElse(null);
                if (tag == null) {
                    continue;
                }

                if (Boolean.TRUE.equals(op.getDelete())) {
                    selectedIds.remove(op.getId());
                    jdbcTemplate.update("delete from comic_tags where tag_id = ?", op.getId());
                    tagRepository.delete(tag);
                    continue;
                }

                if (!name.isBlank()) {
                    tag.setName(name);
                    tagRepository.save(tag);
                }

                if (Boolean.TRUE.equals(op.getSelected())) {
                    selectedIds.add(tag.getId());
                }
                continue;
            }

            if (Boolean.TRUE.equals(op.getDelete()) || name.isBlank()) {
                continue;
            }

            Tag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));

            if (Boolean.TRUE.equals(op.getSelected())) {
                selectedIds.add(tag.getId());
            }
        }

        comic.getTags().clear();
        comic.getTags().addAll(new LinkedHashSet<>(tagRepository.findAllById(selectedIds)));
    }

    private void applyRelationTypeOperations(String json) {
        for (LookupOperation op : parseLookupOperations(json)) {
            String name = optionalTrimmed(op.getName(), 50);

            if (op.getId() != null) {
                RelationType relationType = relationTypeRepository.findById(op.getId()).orElse(null);
                if (relationType == null) {
                    continue;
                }

                if (Boolean.TRUE.equals(op.getDelete())) {
                    jdbcTemplate.update("update comic_relations set relation_type_id = null where relation_type_id = ?", op.getId());
                    relationTypeRepository.delete(relationType);
                    continue;
                }

                if (!name.isBlank()) {
                    relationType.setName(name);
                    relationTypeRepository.save(relationType);
                }
                continue;
            }

            if (Boolean.TRUE.equals(op.getDelete()) || name.isBlank()) {
                continue;
            }

            relationTypeRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> relationTypeRepository.save(RelationType.builder().name(name).build()));
        }
    }

    private void applyRelations(Comic comic, String relationsJson) {
        ensureCollectionsInitialized(comic);

        comic.getRelatedComics().clear();

        Set<Integer> addedComicIds = new LinkedHashSet<>();

        for (RelationInput relationInput : parseRelations(relationsJson)) {
            if (relationInput.getRelatedComicId() == null) {
                continue;
            }
            if (comic.getId() != null && comic.getId().equals(relationInput.getRelatedComicId())) {
                continue;
            }
            if (!addedComicIds.add(relationInput.getRelatedComicId())) {
                continue;
            }

            Comic relatedComic = comicRepository.findById(relationInput.getRelatedComicId()).orElse(null);
            if (relatedComic == null) {
                continue;
            }

            String relationTypeName = optionalTrimmed(relationInput.getRelationTypeName(), 50);
            RelationType relationType = relationTypeName.isBlank()
                    ? null
                    : relationTypeRepository.findByNameIgnoreCase(relationTypeName)
                      .orElseGet(() -> relationTypeRepository.save(RelationType.builder().name(relationTypeName).build()));

            ComicRelation relation = ComicRelation.builder()
                    .comic(comic)
                    .relatedComic(relatedComic)
                    .relationType(relationType)
                    .build();

            comic.getRelatedComics().add(relation);
        }
    }

    private List<LookupOperation> parseLookupOperations(String json) {
        try {
            String actualJson = (json == null || json.isBlank()) ? "[]" : json;
            return objectMapper.readValue(actualJson, new TypeReference<List<LookupOperation>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректные данные формы.");
        }
    }

    private List<RelationInput> parseRelations(String json) {
        try {
            String actualJson = (json == null || json.isBlank()) ? "[]" : json;
            return objectMapper.readValue(actualJson, new TypeReference<List<RelationInput>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректные связанные комиксы.");
        }
    }

    private List<AdminComicRelationItem> parseRelationItemsForView(String json) {
        List<RelationInput> inputs = parseRelations(json);
        List<AdminComicRelationItem> items = new ArrayList<>();

        for (RelationInput input : inputs) {
            if (input.getRelatedComicId() == null) {
                continue;
            }

            Comic comic = comicRepository.findById(input.getRelatedComicId()).orElse(null);
            if (comic == null) {
                continue;
            }

            items.add(new AdminComicRelationItem(
                    comic.getId(),
                    comic.getTitle(),
                    input.getRelationTypeName()
            ));
        }

        return items;
    }

    private void ensureCollectionsInitialized(Comic comic) {
        if (comic.getGenres() == null) {
            comic.setGenres(new LinkedHashSet<>());
        }
        if (comic.getTags() == null) {
            comic.setTags(new LinkedHashSet<>());
        }
        if (comic.getRelatedComics() == null) {
            comic.setRelatedComics(new LinkedHashSet<>());
        }
        if (comic.getParentRelations() == null) {
            comic.setParentRelations(new LinkedHashSet<>());
        }
        if (comic.getChapters() == null) {
            comic.setChapters(new LinkedHashSet<>());
        }
    }

    private String requiredTrimmed(String value, String message, int maxLength) {
        String result = optionalTrimmed(value, maxLength);
        if (result.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String optionalTrimmed(String value, int maxLength) {
        String result = value == null ? "" : value.trim();
        if (maxLength > 0 && result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    private Integer parseReleaseYear(String value) {
        String trimmed = optionalTrimmed(value, 10);
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Укажите год релиза.");
        }

        try {
            int year = Integer.parseInt(trimmed);
            if (year < 1000 || year > 2100) {
                throw new IllegalArgumentException("Год релиза указан некорректно.");
            }
            return year;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Год релиза должен быть числом.");
        }
    }

    private String storeCover(MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            throw new IllegalArgumentException("Загрузите обложку комикса.");
        }

        if (coverFile.getSize() > MAX_COVER_SIZE) {
            throw new IllegalArgumentException("Размер обложки не должен превышать 5 МБ.");
        }

        String originalFilename = coverFile.getOriginalFilename() == null ? "" : coverFile.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        if (!ALLOWED_COVER_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Допустимы только изображения JPG, PNG или WEBP.");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path coversDir = Path.of("src/main/webapp/assets/covers");

        try {
            Files.createDirectories(coversDir);
            Files.copy(coverFile.getInputStream(), coversDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить обложку.");
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    public static class LookupOperation {
        private Integer id;
        private String name;
        private Boolean selected;
        private Boolean delete;
    }

    @Getter
    @Setter
    public static class RelationInput {
        private Integer relatedComicId;
        private String relatedComicTitle;
        private String relationTypeName;
    }
}
