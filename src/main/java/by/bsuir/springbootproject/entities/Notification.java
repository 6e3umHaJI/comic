package by.bsuir.springbootproject.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AttributeOverride(name = "id", column = @Column(name = "notification_id"))
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private NotificationType type;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "comic_id")
    private Comic comic;

    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne
    @JoinColumn(name = "translation_id")
    private Translation translation;

    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "link_path", length = 255)
    private String linkPath;

    @Column(name = "is_clickable", nullable = false)
    private Boolean isClickable = false;

    @Column(name = "comic_title_snapshot", length = 255)
    private String comicTitleSnapshot;

    @Column(name = "chapter_number_snapshot")
    private Integer chapterNumberSnapshot;

    @Column(name = "language_name_snapshot", length = 100)
    private String languageNameSnapshot;

    @Column(name = "actor_username_snapshot", length = 100)
    private String actorUsernameSnapshot;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.format(DATE_TIME_FORMATTER) : "";
    }

    public String getCreatedAtIso() {
        return createdAt != null ? createdAt.format(ISO_DATE_TIME_FORMATTER) : "";
    }
}
