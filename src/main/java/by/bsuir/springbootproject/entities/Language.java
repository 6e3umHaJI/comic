package by.bsuir.springbootproject.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "language_id"))
@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Language extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "ocr_space_code", length = 20)
    private String ocrSpaceCode;

    @Column(name = "mymemory_code", length = 10)
    private String mymemoryCode;
}
