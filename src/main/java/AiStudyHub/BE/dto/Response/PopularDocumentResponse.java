package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PopularDocumentResponse {
    Long documentId;
    String title;
    String ownerName;
    String subjectName;
    long downloadCount;
    double averageRating;
}
