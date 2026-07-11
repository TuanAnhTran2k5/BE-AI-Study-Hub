package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.AppealStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long appealId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caseId", nullable = false)
    ReportCase reportCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    String appealReason;

    String evidenceUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    AppealStatus status = AppealStatus.PENDING;

    LocalDateTime createdAt;

    LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvedBy")
    User resolvedBy;

    @Column(columnDefinition = "TEXT")
    String adminNote;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
