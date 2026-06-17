package AiStudyHub.BE.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookmarkRequest {

    @NotNull(message = "FIELD_REQUIRED")
    Long documentId;
}
