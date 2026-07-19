package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboSubjectResponse {
    Long comboId;
    String comboCode;
    String comboName;
    List<SubjectResponse> subjects;
    Boolean isDeleted;
}
