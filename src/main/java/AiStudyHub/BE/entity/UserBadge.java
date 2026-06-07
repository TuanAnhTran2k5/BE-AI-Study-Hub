package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_badge",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_badge",
                        columnNames = {"userId", "badgeId"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long userBadgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badgeId", nullable = false)
    Badge badge;

    LocalDateTime achievedAt;

    @PrePersist
    public void prePersist() {
        achievedAt = LocalDateTime.now();
    }
}