package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "semester")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long semesterId;

    @Column(nullable = false, unique = true)
    String semesterNo;

    @Column(columnDefinition = "TEXT")
    String description;
}