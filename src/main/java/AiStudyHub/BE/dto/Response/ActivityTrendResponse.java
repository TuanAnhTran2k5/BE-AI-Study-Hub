package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityTrendResponse {
    LocalDate date;
    long newUsers;
    long newDocuments;
    long newDownloads;
    long aiQueries;
}
