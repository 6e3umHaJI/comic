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

import java.time.LocalDateTime;

@AttributeOverride(name = "id", column = @Column(name = "usage_id"))
@Entity
@Table(name = "api_monthly_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ApiMonthlyUsage extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "month_key", nullable = false, length = 7)
    private String monthKey;

    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;

    @Column(name = "requests_used", nullable = false)
    private Integer requestsUsed;

    @Column(name = "char_limit", nullable = false)
    private Integer charLimit;

    @Column(name = "chars_used", nullable = false)
    private Integer charsUsed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
