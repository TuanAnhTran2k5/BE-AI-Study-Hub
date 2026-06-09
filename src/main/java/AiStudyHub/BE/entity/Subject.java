package AiStudyHub.BE.entity;

import AiStudyHub.BE.constraint.SubjectType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "subject",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_code", columnNames = "subjectCode")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semesterId", nullable = false)
    Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comboId")
    ComboSubject comboSubject;

    String subjectCode;
    String subjectName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SubjectType subjectType;
}