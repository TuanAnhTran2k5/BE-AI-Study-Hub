package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "score_type",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_score_type_code", columnNames = "typeCode")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long scoreTypeId;

    String typeCode;
    String typeName;
    Integer defaultPoint;

    @Column(columnDefinition = "TEXT")
    String description;
}
