package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.SubjectType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectResponse {
    Long subjectId;
    String subjectCode;
    String subjectName;
    SubjectType subjectType;

    String description;

    Long semesterId;
    String semesterNo;

    Long comboId;
    String comboCode;
    String comboName;

    Boolean isDeleted;
}