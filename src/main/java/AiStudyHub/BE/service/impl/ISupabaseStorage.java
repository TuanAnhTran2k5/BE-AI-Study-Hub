package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;


public interface ISupabaseStorage {

    FileUploadResponse uploadFile(MultipartFile file, String folder) throws Exception;

    String deleteFile(String fileUrlPath);

    byte[] downloadFile(String fileUrlPath);

    FileUploadResponse uploadBytes(
            byte[] data,
            String originalFileName,
            String folder,
            String contentType
    ) throws Exception;

}
