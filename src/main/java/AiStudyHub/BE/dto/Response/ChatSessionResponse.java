package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionResponse {
    Long sessionId;
    String sessionTitle;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<Long> documentIds;
}
