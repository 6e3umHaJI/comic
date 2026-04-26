package by.bsuir.springbootproject.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "auto_translation_preview_page_id"))
@Entity
@Table(name = "auto_translation_preview_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AutoTranslationPreviewPage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_id", nullable = false)
    private AutoTranslationPreview preview;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Builder.Default
    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected = false;

    @Builder.Default
    @Column(name = "is_translated", nullable = false)
    private Boolean isTranslated = false;

    @Column(name = "source_image_path", nullable = false, length = 255)
    private String sourceImagePath;

    @Column(name = "result_image_path", nullable = false, length = 255)
    private String resultImagePath;
}
