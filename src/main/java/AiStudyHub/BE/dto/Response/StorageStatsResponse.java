package AiStudyHub.BE.dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageStatsResponse {
    long totalStorageUsed;
    long totalActiveDocuments;
    double averageDocumentSize;
    Map<String, Long> fileTypeDistribution;
}
