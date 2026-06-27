package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestPromptsRequest {
    @NotEmpty(message = "Document IDs cannot be empty")
    private List<Long> documentIds;
}
