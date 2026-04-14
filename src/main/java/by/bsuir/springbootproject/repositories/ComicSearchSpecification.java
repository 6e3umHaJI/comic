package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.Chapter;
import by.bsuir.springbootproject.entities.Comic;
import by.bsuir.springbootproject.entities.Genre;
import by.bsuir.springbootproject.entities.Language;
import by.bsuir.springbootproject.entities.ReviewStatus;
import by.bsuir.springbootproject.entities.SearchCriteria;
import by.bsuir.springbootproject.entities.Tag;
import by.bsuir.springbootproject.entities.Translation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class ComicSearchSpecification implements Specification<Comic> {

    private final SearchCriteria criteria;

    @Override
    public Predicate toPredicate(
            @NonNull Root<Comic> root,
            @NonNull CriteriaQuery<?> query,
            @NonNull CriteriaBuilder cb
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getKeyWords() != null && !criteria.getKeyWords().isBlank()) {
            String keyword = "%" + criteria.getKeyWords().toLowerCase() + "%";

            predicates.add(
                    cb.or(
                            cb.like(cb.lower(root.get("title")), keyword),
                            cb.like(cb.lower(root.get("originalTitle")), keyword)
                    )
            );
        }

        String[] selectedGenres = cleanValues(criteria.getSelectedGenres());
        if (selectedGenres.length > 0) {
            Join<Comic, Genre> genreJoin = root.join("genres", JoinType.LEFT);
            CriteriaBuilder.In<String> inGenres = cb.in(cb.lower(genreJoin.get("name")));

            for (String genre : selectedGenres) {
                inGenres.value(genre.toLowerCase());
            }

            if (criteria.isStrictGenreMatch()) {
                for (String genre : selectedGenres) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Comic> subRoot = sq.from(Comic.class);
                    Join<Comic, Genre> subGenre = subRoot.join("genres");

                    sq.select(cb.literal(1L)).where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            cb.equal(cb.lower(subGenre.get("name")), genre.toLowerCase())
                    );

                    predicates.add(cb.exists(sq));
                }
            } else {
                predicates.add(inGenres);
            }
        }

        String[] selectedTags = cleanValues(criteria.getSelectedTags());
        if (selectedTags.length > 0) {
            Join<Comic, Tag> tagJoin = root.join("tags", JoinType.LEFT);
            CriteriaBuilder.In<String> inTags = cb.in(cb.lower(tagJoin.get("name")));

            for (String tag : selectedTags) {
                inTags.value(tag.toLowerCase());
            }

            if (criteria.isStrictTagMatch()) {
                for (String tag : selectedTags) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Comic> subRoot = sq.from(Comic.class);
                    Join<Comic, Tag> subTag = subRoot.join("tags");

                    sq.select(cb.literal(1L)).where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            cb.equal(cb.lower(subTag.get("name")), tag.toLowerCase())
                    );

                    predicates.add(cb.exists(sq));
                }
            } else {
                predicates.add(inTags);
            }
        }

        String[] selectedLanguages = cleanValues(criteria.getSelectedLanguages());
        if (selectedLanguages.length > 0) {
            Subquery<Integer> languageSubquery = query.subquery(Integer.class);
            Root<Translation> translationRoot = languageSubquery.from(Translation.class);
            Join<Translation, Chapter> chapterJoin = translationRoot.join("chapter");
            Join<Translation, Language> languageJoin = translationRoot.join("language");
            Join<Translation, ReviewStatus> reviewStatusJoin = translationRoot.join("reviewStatus");

            languageSubquery.select(chapterJoin.get("comic").get("id"));
            languageSubquery.where(
                    cb.equal(chapterJoin.get("comic").get("id"), root.get("id")),
                    cb.equal(reviewStatusJoin.get("name"), "Одобрено"),
                    languageJoin.get("name").in((Object[]) selectedLanguages)
            );
            languageSubquery.groupBy(chapterJoin.get("comic").get("id"));

            if (criteria.isStrictLanguageMatch()) {
                languageSubquery.having(
                        cb.equal(
                                cb.countDistinct(languageJoin.get("name")),
                                selectedLanguages.length
                        )
                );
            }

            predicates.add(cb.exists(languageSubquery));
        }

        String[] selectedTypes = cleanValues(criteria.getSelectedTypes());
        if (selectedTypes.length > 0) {
            Join<Object, Object> typeJoin = root.join("type", JoinType.LEFT);
            CriteriaBuilder.In<Object> inTypes = cb.in(typeJoin.get("name"));

            for (String type : selectedTypes) {
                inTypes.value(type);
            }

            predicates.add(inTypes);
        }

        String[] selectedStatuses = cleanValues(criteria.getSelectedComicStatuses());
        if (selectedStatuses.length > 0) {
            Join<Object, Object> statusJoin = root.join("comicStatus", JoinType.LEFT);
            CriteriaBuilder.In<Object> inStatuses = cb.in(statusJoin.get("name"));

            for (String status : selectedStatuses) {
                inStatuses.value(status);
            }

            predicates.add(inStatuses);
        }

        String[] selectedAgeRatings = cleanValues(criteria.getSelectedAgeRatings());
        if (selectedAgeRatings.length > 0) {
            Join<Object, Object> ageJoin = root.join("ageRating", JoinType.LEFT);
            CriteriaBuilder.In<Object> inAges = cb.in(ageJoin.get("name"));

            for (String age : selectedAgeRatings) {
                inAges.value(age);
            }

            predicates.add(inAges);
        }

        if (criteria.getReleaseYearFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("releaseYear"), criteria.getReleaseYearFrom()));
        }

        if (criteria.getReleaseYearTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("releaseYear"), criteria.getReleaseYearTo()));
        }

        if (criteria.getRatingsCountFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("ratingsCount"), criteria.getRatingsCountFrom()));
        }

        if (criteria.getRatingsCountTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("ratingsCount"), criteria.getRatingsCountTo()));
        }

        if (criteria.getAvgRatingFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("avgRating"), criteria.getAvgRatingFrom()));
        }

        if (criteria.getAvgRatingTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("avgRating"), criteria.getAvgRatingTo()));
        }

        if (criteria.getChaptersCountFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("chaptersCount"), criteria.getChaptersCountFrom()));
        }

        if (criteria.getChaptersCountTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("chaptersCount"), criteria.getChaptersCountTo()));
        }

        if (criteria.getUpdatedFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedFrom().atStartOfDay()));
        }

        if (criteria.getUpdatedTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedTo().atTime(23, 59, 59)));
        }

        query.distinct(true);
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private String[] cleanValues(String[] values) {
        if (values == null) {
            return new String[0];
        }

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }
}
