package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusResponse {
    Long id;
    Long subjectId;
    String subjectCode;
    String subjectName;
    String pdfUrl;
    String jsonContent;
    String syncStatus;
    String parserVersion;
    String embeddingModel;
    Integer embeddingVersion;
    LocalDateTime updatedAt;
}
