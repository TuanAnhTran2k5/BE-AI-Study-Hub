package AiStudyHub.BE.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "combo_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long comboId;

    String comboCode;
    String comboName;
}