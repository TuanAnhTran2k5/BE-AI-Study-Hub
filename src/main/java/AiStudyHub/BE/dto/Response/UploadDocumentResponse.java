package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO containing the details of an uploaded document.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadDocumentResponse {

    Long id;
    String originalFileName;
    String contentType;
    Long fileSize;
    String uploadedBy;
    LocalDateTime uploadDate;
    String status;
}
