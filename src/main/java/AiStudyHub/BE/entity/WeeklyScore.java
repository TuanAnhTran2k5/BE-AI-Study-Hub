package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "weekly_score",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_weekly_score_user_week",
                        columnNames = {"userId", "weekStart"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long weeklyScoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    LocalDate weekStart;
    Integer score = 0;
    String rankPosition;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}