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
    Long storageBonus;
    String displayPriority;
    String iconUrl;
    String color;
    String badgeColor;
    String textColor;
}
