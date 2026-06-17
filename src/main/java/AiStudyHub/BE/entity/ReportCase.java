package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.CaseStatus;
import AiStudyHub.BE.constraint.ReportSeverity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reasonId")
    ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ReportSeverity caseLevel;

    @Builder.Default
    Integer reportCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    CaseStatus caseStatus = CaseStatus.OPEN;

    LocalDateTime openedAt;

    LocalDateTime firstWarningAt;

    LocalDateTime secondWarningAt;

    LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        openedAt = LocalDateTime.now();
    }
}