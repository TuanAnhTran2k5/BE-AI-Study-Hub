package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusHistoryResponse {
    Long id;
    Long subjectSyllabusId;
    String pdfUrl;
    String jsonContent;
    Integer version;
    String updatedBy;
    String updatedReason;
    LocalDateTime createdAt;
}
