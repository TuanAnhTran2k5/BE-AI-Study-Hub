package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookmarkResponse {

    Long bookmarkId;
    Long userId;
    Long documentId;
    String documentTitle;
    LocalDateTime bookmarkedAt;
}
