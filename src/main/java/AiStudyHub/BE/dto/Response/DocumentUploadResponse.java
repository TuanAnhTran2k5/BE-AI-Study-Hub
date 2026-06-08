package AiStudyHub.BE.dto.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DocumentUploadResponse {

    private Long documentId;

    private Long ownerId;

    private String title;

    private String fileUrl;

    private String message;
}