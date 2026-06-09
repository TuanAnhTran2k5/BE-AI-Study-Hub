package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;


// Metadata returned after a file is successfully uploaded to Supabase Storage.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {

    // Original file name sent by the client
    String originalFileName;

    // File name stored in Supabase (with a UUID prefix)
    String storedFileName;

    // Full path inside the bucket (folder/storedFileName)
    String storagePath;

    // Public URL to access the file
    String publicUrl;

    // MIME type of the file
    String contentType;

    // File size in bytes
    Long fileSize;
}
