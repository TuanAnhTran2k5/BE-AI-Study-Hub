package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.SubjectType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectRequest {
    String subjectCode;
    String subjectName;
    String description;
    SubjectType subjectType;
    Long semesterId;
    Long comboId; // Optional
}
