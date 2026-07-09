package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.UserRole;
import AiStudyHub.BE.constraint.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse {
    Long userId;
    String fullName;
    String email;
    String avatarUrl;
    UserRole role;
    UserStatus status;
    Long totalScore;
    LocalDateTime createdAt;
    
    // Ban info
    String banReason;
    LocalDateTime bannedAt;
    String bannedByName;
    
    // Quick stats (batch-loaded, NOT N+1)
    long activeDocumentCount;
    long documentDownloadsReceived;
}
