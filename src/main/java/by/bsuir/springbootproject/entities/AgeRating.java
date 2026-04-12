package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@AttributeOverride(name = "id", column = @Column(name = "age_rating_id"))
@Entity
@Table(name = "age_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AgeRating extends BaseEntity {

    @Column(nullable = false, length = 5)
    private String name;
}