package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
    @Builder.Default
    Long totalScore = 0L;
    String email;

    UserRole role;

    @Builder.Default
    Long storageUsed = 0L;
    @Builder.Default
    Long storageLimit = 0L;

    UserRankResponse rank;
    List<UserBadgeResponse> badges;
}
