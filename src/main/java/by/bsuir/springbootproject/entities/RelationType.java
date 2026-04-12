package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "relation_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@AttributeOverride(name = "id", column = @Column(name = "relation_type_id"))
public class RelationType extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;
}