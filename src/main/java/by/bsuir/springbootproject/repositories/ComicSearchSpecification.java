package by.bsuir.springbootproject.repositories;

import by.bsuir.springbootproject.entities.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ComicSearchSpecification implements Specification<Comic> {

    private final SearchCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull Root<Comic> root,
                                 @NonNull CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getKeyWords() != null && !criteria.getKeyWords().isBlank()) {
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + criteria.getKeyWords().toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("originalTitle")), "%" + criteria.getKeyWords().toLowerCase() + "%")
            ));
        }

        if (criteria.getSelectedGenres() != null && criteria.getSelectedGenres().length > 0) {
            Join<Comic, Genre> g = root.join("genres", JoinType.LEFT);
            CriteriaBuilder.In<String> inGenres = cb.in(cb.lower(g.get("name")));
            for (String genre : criteria.getSelectedGenres()) {
                inGenres.value(genre.toLowerCase());
            }

            if (criteria.isStrictGenreMatch()) {
                for (String genre : criteria.getSelectedGenres()) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Comic> subRoot = sq.from(Comic.class);
                    Join<Comic, Genre> subG = subRoot.join("genres");
                    sq.select(cb.literal(1L))
                            .where(
                                    cb.equal(subRoot.get("id"), root.get("id")),
                                    cb.equal(cb.lower(subG.get("name")), genre.toLowerCase())
                            );
                    predicates.add(cb.exists(sq));
                }
            } else {
                predicates.add(inGenres);
            }
        }

        if (criteria.getSelectedTags() != null && criteria.getSelectedTags().length > 0) {
            Join<Comic, Tag> t = root.join("tags", JoinType.LEFT);
            CriteriaBuilder.In<String> inTags = cb.in(cb.lower(t.get("name")));
            for (String tag : criteria.getSelectedTags()) {
                inTags.value(tag.toLowerCase());
            }

            if (criteria.isStrictTagMatch()) {
                for (String tag : criteria.getSelectedTags()) {
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<Comic> subRoot = sq.from(Comic.class);
                    Join<Comic, Tag> subT = subRoot.join("tags");
                    sq.select(cb.literal(1L)).where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            cb.equal(cb.lower(subT.get("name")), tag.toLowerCase())
                    );
                    predicates.add(cb.exists(sq));
                }
            } else {
                predicates.add(inTags);
            }
        }

        if (criteria.getSelectedLanguages() != null && criteria.getSelectedLanguages().length > 0) {
            Subquery<Integer> languageSubquery = query.subquery(Integer.class);
            Root<Translation> tr = languageSubquery.from(Translation.class);
            Join<Translation, Chapter> ch = tr.join("chapter");
            Join<Translation, Language> lang = tr.join("language");
            Join<Translation, ReviewStatus> rs = tr.join("reviewStatus");

            languageSubquery.select(ch.get("comic").get("id"));
            languageSubquery.where(
                    cb.equal(ch.get("comic").get("id"), root.get("id")),
                    cb.equal(rs.get("name"), "Одобрено"),
                    lang.get("name").in((Object[]) criteria.getSelectedLanguages())
            );
            languageSubquery.groupBy(ch.get("comic").get("id"));

            if (criteria.isStrictLanguageMatch()) {
                languageSubquery.having(
                        cb.equal(
                                cb.countDistinct(lang.get("name")),
                                criteria.getSelectedLanguages().length
                        )
                );
            }

            predicates.add(cb.exists(languageSubquery));
        }


        if (criteria.getSelectedTypes() != null && criteria.getSelectedTypes().length > 0) {
            Join<Comic, ComicType> type = root.join("type", JoinType.LEFT);
            CriteriaBuilder.In<String> inTypes = cb.in(type.get("name"));
            for (String t : criteria.getSelectedTypes())
                inTypes.value(t);
            predicates.add(inTypes);
        }

        if (criteria.getSelectedComicStatuses() != null && criteria.getSelectedComicStatuses().length > 0) {
            Join<Comic, ComicStatus> st = root.join("comicStatus", JoinType.LEFT);
            CriteriaBuilder.In<String> inComicStatuses = cb.in(st.get("name"));
            for (String s : criteria.getSelectedComicStatuses())
                inComicStatuses.value(s);
            predicates.add(inComicStatuses);
        }

        if (criteria.getSelectedAgeRatings() != null && criteria.getSelectedAgeRatings().length > 0) {
            Join<Comic, AgeRating> ar = root.join("ageRating", JoinType.LEFT);
            CriteriaBuilder.In<String> inAges = cb.in(ar.get("name"));
            for (String a : criteria.getSelectedAgeRatings())
                inAges.value(a);
            predicates.add(inAges);
        }

        if (criteria.getReleaseYearFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("releaseYear"), criteria.getReleaseYearFrom()));
        if (criteria.getReleaseYearTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("releaseYear"), criteria.getReleaseYearTo()));

        if (criteria.getRatingsCountFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("ratingsCount"), criteria.getRatingsCountFrom()));
        if (criteria.getRatingsCountTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("ratingsCount"), criteria.getRatingsCountTo()));

        if (criteria.getAvgRatingFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("avgRating"), criteria.getAvgRatingFrom()));
        if (criteria.getAvgRatingTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("avgRating"), criteria.getAvgRatingTo()));

        if (criteria.getChaptersCountFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("chaptersCount"), criteria.getChaptersCountFrom()));
        if (criteria.getChaptersCountTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("chaptersCount"), criteria.getChaptersCountTo()));

        if (criteria.getUpdatedFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedFrom().atStartOfDay()));
        if (criteria.getUpdatedTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedTo().atTime(23, 59, 59)));

        query.distinct(true);
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}