package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardEntry {
    Integer rank;
    Long userId;
    String fullName;
    String avatarUrl;
    Long totalScore;
    String rankName;
    String rankIcon;
    String rankColor;
    String rankTextColor;
}
