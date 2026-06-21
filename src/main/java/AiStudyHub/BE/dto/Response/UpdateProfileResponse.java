package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

import AiStudyHub.BE.constraint.UserStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProfileResponse {
    Long userId;

    String fullName;
    String avatarUrl;
    Long totalScore;
    String email;

    UserRole role;
    UserStatus status;

    Long storageUsed;
    Long storageLimit;

    UserRankResponse rank;
    List<UserBadgeResponse> badges;
}
