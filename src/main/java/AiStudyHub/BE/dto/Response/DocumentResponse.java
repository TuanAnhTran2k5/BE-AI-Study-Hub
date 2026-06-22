package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.ModerationStatus;
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
public class DocumentResponse {

    Long documentId;

    Long ownerId;
    String ownerName;
    String ownerAvatar;

    Long subjectId;
    String subjectCode;
    String subjectName;

    String title;

    String fileName;
    String fileUrl;
    String fileType;
    Long fileSize;

    VisibilityStatus visibilityStatus;
    ModerationStatus moderationStatus;

    Double averageRating;
    Integer ratingCount;

    Integer downloadCount;
    Integer bookmarkCount;
    Integer reportCount;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
