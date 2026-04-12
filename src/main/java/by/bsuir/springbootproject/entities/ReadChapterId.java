package by.bsuir.springbootproject.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReadChapterId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "chapter_id")
    private Integer chapterId;
}
