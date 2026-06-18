package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ranking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long rankId;

    String rankName;
    Integer minScore;
    Integer maxScore;
    @Builder.Default
    Long storageBonus = 0L;
    String displayPriority;
}
