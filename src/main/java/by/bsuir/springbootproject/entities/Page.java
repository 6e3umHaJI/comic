package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "page_id"))
@Entity
@Table(name = "pages")
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