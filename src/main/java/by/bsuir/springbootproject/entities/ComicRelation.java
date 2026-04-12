package by.bsuir.springbootproject.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "comic_relations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comic_id", "related_comic_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@AttributeOverride(name = "id", column = @Column(name = "relation_id"))
public class ComicRelation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id", nullable = false)
    private Comic comic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_comic_id", nullable = false)
    private Comic relatedComic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relation_type_id")
    private RelationType relationType;
}