package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AttributeOverride(name = "id", column = @Column(name = "translation_id"))
@Entity
@Table(name = "translations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Translation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne
    @JoinColumn(name = "language_id")
    private Language language;

    @ManyToOne
    @JoinColumn(name = "translation_type_id")
    private TranslationType translationType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    @Column(length = 255)
    private String title;

    @ManyToOne
    @JoinColumn(name = "review_status_id")
    private ReviewStatus reviewStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.format(DATE_TIME_FORMATTER) : "";
    }

    public String getCreatedAtIso() {
        return createdAt != null ? createdAt.format(ISO_DATE_TIME_FORMATTER) : "";
    }
}