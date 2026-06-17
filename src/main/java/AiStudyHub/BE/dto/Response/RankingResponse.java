package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RankingResponse {
    Long rankId;
    String rankName;
    Integer minScore;
    Integer maxScore;
    Integer storageBonus;
    String displayPriority;
}
