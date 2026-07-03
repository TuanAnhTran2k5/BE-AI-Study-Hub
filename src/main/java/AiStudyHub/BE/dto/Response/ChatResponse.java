package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    Long sessionId;
    String sessionTitle;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<Long> documentIds;
    String answer;
    List<String> sources;
}
