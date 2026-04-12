package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "page_id"))
@Entity
@Table(name = "pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComicPage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_path", nullable = false, columnDefinition = "TEXT")
    private String imagePath;
}