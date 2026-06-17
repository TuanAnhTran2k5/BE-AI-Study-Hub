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
public class DocumentDownloadResponse {

    Long documentId;

    String title;

    String fileName;

    String fileType;

    Long fileSize;

    Long ownerId;

    String ownerName;

    Boolean firstDownload;

    Integer addedPoint;

    Long ownerTotalScore;

    String publicOwnerName;

    LocalDateTime downloadedAt;
}
