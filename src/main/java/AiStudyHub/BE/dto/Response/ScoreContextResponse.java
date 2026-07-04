package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.service.IGamification.ScoreTypeSpec;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreContextResponse {
    Long receiverUserId;
    Long documentId;
    String documentTitle;
    ScoreTypeSpec spec;
    int scoreChange;
    String description;
    Long actorUserId;
}
