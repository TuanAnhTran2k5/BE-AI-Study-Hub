package AiStudyHub.BE.dto.Request;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {
    private List<Long> documentIds;
}
