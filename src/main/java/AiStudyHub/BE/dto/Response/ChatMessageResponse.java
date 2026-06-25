package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.SenderType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    Long messageId;
    SenderType senderType;
    String content;
    LocalDateTime createdAt;
}
