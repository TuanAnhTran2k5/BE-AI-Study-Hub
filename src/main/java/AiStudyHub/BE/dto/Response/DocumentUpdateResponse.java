package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.VisibilityStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentUpdateResponse {

    Long documentId;

    String title;

    Long subjectId;

    VisibilityStatus visibilityStatus;

    LocalDateTime updatedAt;
}
