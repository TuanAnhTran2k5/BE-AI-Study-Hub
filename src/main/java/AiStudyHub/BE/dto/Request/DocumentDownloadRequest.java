package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentDownloadRequest {
    byte[] fileBytes;

    String fileName;

    String encodedFileName;

    MediaType mediaType;

    DocumentDownloadResponse metadata;
}
