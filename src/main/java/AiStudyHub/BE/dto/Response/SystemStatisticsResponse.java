package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemStatisticsResponse {
    StatisticCard totalActiveUsers;
    StatisticCard totalDocuments;
    StatisticCard totalDownloads;
    StatisticCard totalAiQueries;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StatisticCard {
        long value;
        String growthRate;
    }
}
