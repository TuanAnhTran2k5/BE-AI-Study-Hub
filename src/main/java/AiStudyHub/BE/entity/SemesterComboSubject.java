package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "semester_combo_subject",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_semester_combo",
                        columnNames = {"semesterId", "comboId"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterComboSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long semesterComboSubjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semesterId", nullable = false)
    Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comboId", nullable = false)
    ComboSubject combo;
}
