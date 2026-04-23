package by.bsuir.springbootproject.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ReadProgressId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "translation_id", nullable = false)
    private Integer translationId;
}
