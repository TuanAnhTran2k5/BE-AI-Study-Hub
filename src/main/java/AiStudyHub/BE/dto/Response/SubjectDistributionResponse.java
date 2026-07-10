package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectDistributionResponse {
    String subjectName;
    long documentCount;
    double percentage;
}
