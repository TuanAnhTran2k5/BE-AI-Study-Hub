package AiStudyHub.BE.dto.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyScoreResponse {
    Long userId;
    String fullName;
    String avatarUrl;
    String email;
    Integer score;
    LocalDate weekStart;
}
