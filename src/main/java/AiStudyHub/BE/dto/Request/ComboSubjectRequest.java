package AiStudyHub.BE.dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboSubjectRequest {
    String comboCode;
    String comboName;
    List<SubjectRequest> subjects; // Required on create, optional on update
}
