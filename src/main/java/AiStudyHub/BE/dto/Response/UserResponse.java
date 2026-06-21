package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import AiStudyHub.BE.constraint.UserStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    Long userId;

    String fullName;
    String avatarUrl;
    @Builder.Default
    Long totalScore = 0L;
    String email;
    UserRole role;
    UserStatus status;

    @Builder.Default
    Long storageUsed = 0L;
    @Builder.Default
    Long storageLimit = 0L;

    String accessToken;
}
