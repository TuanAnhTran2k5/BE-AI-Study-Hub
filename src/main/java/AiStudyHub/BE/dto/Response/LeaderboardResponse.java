package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardResponse {
    Integer rank;
    Long userId;
    String fullName;
    String avatarUrl;
    Long totalScore;
    String rankName;
}
