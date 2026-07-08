package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BadgeResponse {
    Long badgeId;
    String badgeName;
    String description;
    String conditionText;
    String iconUrl;
    Integer requiredDownloads;
}
