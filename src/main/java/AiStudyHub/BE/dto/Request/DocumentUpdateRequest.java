package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.VisibilityStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentUpdateRequest {

    String title;

    Long subjectId;

    VisibilityStatus visibilityStatus;
}
