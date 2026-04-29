package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "page_id"))
@Entity
@Table(
        name = "pages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pages_translation_page",
                        columnNames = {"translation_id", "page_number"}
                ),
                @UniqueConstraint(
                        name = "uq_pages_image_path",
                        columnNames = {"image_path"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Page extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "translation_id")
    private Translation translation;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;
}