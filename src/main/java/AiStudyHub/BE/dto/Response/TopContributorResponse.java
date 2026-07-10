package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopContributorResponse {
    Long userId;
    String fullName;
    String avatarUrl;
    Long totalScore;
    long activeDocumentCount;
    String rankName;
}
