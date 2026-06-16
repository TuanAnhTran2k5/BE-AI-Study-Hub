package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRankResponse {
    Long userRankId;
    Long userId;
    RankingResponse rank;
    LocalDateTime achievedAt;
    LocalDateTime updatedAt;
}
