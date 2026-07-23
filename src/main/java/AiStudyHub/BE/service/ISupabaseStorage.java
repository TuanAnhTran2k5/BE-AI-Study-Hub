package AiStudyHub.BE.service;

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

    /**
     * Upload file vào một bucket Supabase được chỉ định.
     * Dùng để upload avatar vào bucket "Avatars" thay vì bucket "Documents" mặc định.
     *
     * @param file   file cần upload
     * @param folder thư mục con trong bucket (null nếu để root)
     * @param bucket tên bucket Supabase đích
     */
    FileUploadResponse uploadFileToBucket(MultipartFile file, String folder, String bucket) throws Exception;

    /**
     * Download ảnh từ URL bên ngoài (ví dụ: Google avatar URL) và upload vào bucket Supabase chỉ định.
     *
     * @param imageUrl URL công khai của ảnh cần tải
     * @param folder   thư mục con trong bucket
     * @param bucket   tên bucket Supabase đích
     */
    FileUploadResponse downloadAndUploadToBucket(String imageUrl, String folder, String bucket) throws Exception;

}

