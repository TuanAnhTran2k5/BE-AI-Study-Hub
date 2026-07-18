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

    Long originalUploaderId;
    String originalUploaderName;
    String originalUploaderAvatar;

    Long ownerTotalScore;
    AiStudyHub.BE.dto.Response.UserRankResponse ownerCurrentRank;
    Integer ownerDocumentCount;
    Integer ownerDownloadCount;

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
    AiStudyHub.BE.constraint.UploadStatus uploadStatus;

    Double averageRating;
    Integer ratingCount;

    Integer downloadCount;
    Integer bookmarkCount;
    Integer reportCount;

    Boolean isBookmarked;
    Integer myRating;

    String moderatedByEmail;
    String moderationNote;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
