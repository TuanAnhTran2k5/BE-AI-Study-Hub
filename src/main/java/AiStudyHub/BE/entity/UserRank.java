package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_rank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long userRankId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rankId", nullable = false)
    Ranking rank;

    LocalDateTime achievedAt;
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        achievedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
