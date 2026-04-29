package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@AttributeOverride(name = "id", column = @Column(name = "comic_id"))
@Entity
@Table(name = "comics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Comic extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private ComicType type;

    @ManyToOne
    @JoinColumn(name = "age_rating_id")
    private AgeRating ageRating;

    @Column(name = "release_year", nullable = false)
    private Integer releaseYear;

    @ManyToOne
    @JoinColumn(name = "comic_status_id", nullable = false)
    private ComicStatus comicStatus;

    @Column(name = "short_description", nullable = false, length = 500)
    private String shortDescription;

    @Column(name = "full_description", nullable = false, length = 2000)
    private String fullDescription;

    @Column(nullable = false, length = 255)
    private String cover;

    @Builder.Default
    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Chapter> chapters = new HashSet<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "popularity_score", nullable = false)
    private Long popularityScore = 0L;

    @Builder.Default
    @Column(name = "ratings_count", nullable = false)
    private Integer ratingsCount = 0;

    @Builder.Default
    @Column(name = "chapters_count", nullable = false)
    private Integer chaptersCount = 0;

    @Builder.Default
    @Column(name = "avg_rating", nullable = false)
    private Double avgRating = 0.0;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "comic_tags",
            joinColumns = @JoinColumn(name = "comic_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "comic_genres",
            joinColumns = @JoinColumn(name = "comic_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ComicRelation> relatedComics = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "relatedComic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ComicRelation> parentRelations = new HashSet<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getFormattedUpdatedAt() {
        return updatedAt != null ? updatedAt.toLocalDate().toString() : "";
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.format(DATE_FORMATTER) : "";
    }

    public String getUpdatedAtFormatted() {
        return updatedAt != null ? updatedAt.format(DATE_FORMATTER) : "";
    }

    public String getCreatedAtIso() {
        return createdAt != null ? createdAt.format(ISO_DATE_TIME_FORMATTER) : "";
    }

    public String getUpdatedAtIso() {
        return updatedAt != null ? updatedAt.format(ISO_DATE_TIME_FORMATTER) : "";
    }
}
