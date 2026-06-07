package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.ReportSeverity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "report_reason")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long reasonId;

    String reasonName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ReportSeverity severityLevel;

    @Column(columnDefinition = "TEXT")
    String description;
}
