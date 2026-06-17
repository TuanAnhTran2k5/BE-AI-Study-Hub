package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO containing the RAG indexing details of a document.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagDocumentResponse {

    Long id;
    String originalFileName;
    String contentType;
    Long fileSize;
    String uploadedBy;
    LocalDateTime uploadDate;
    String status;
}
