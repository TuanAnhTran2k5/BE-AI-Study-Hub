package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO for a single document chunk's metadata and content.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagChunkResponse {

    Long id;
    Long documentId;
    Integer chunkIndex;
    String content;
    Boolean embeddingCreated;
    LocalDateTime createdAt;
}
