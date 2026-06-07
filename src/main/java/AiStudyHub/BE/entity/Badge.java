package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "badge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long badgeId;

    String badgeName;

    @Column(columnDefinition = "TEXT")
    String description;

    String conditionText;
    String iconUrl;
}
