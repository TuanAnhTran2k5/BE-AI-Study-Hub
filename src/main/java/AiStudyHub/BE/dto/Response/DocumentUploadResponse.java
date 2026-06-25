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
public class DocumentUploadResponse {

    Long documentId;

    Long ownerId;

    Long subjectId;

    String title;

    String fileName;

    String fileUrl;

    String fileType;

    Long fileSize;

    VisibilityStatus visibilityStatus;

    AiStudyHub.BE.constraint.UploadStatus uploadStatus;

    LocalDateTime createdAt;

    String message;
}