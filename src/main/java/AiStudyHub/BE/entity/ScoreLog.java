package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long scoreLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    User user;

    @Column(name = "documentId")
    Long documentId;

    @Column(name = "documentTitle")
    String documentTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoreTypeId", nullable = false)
    ScoreType scoreType;

    @Column(name = "actorUserId")
    Long actorUserId;

    @Column(name = "unique_action_key", unique = true)
    String uniqueActionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportId")
    Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportCaseId")
    ReportCase reportCase;

    Integer scoreChange;

    @Column(columnDefinition = "TEXT")
    String description;

    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
