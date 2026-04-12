package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@AttributeOverride(name = "id", column = @Column(name = "chapter_id"))
@Entity
@Table(name = "chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Chapter extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "comic_id")
    private Comic comic;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Translation> translations = new HashSet<>();
}