package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.impl.ISupabaseStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
    
@Service
public class SupabaseStoreService implements ISupabaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStoreService.class);

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket}")
    private String supabaseBucket;

    public SupabaseStoreService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String storedFileName    = UUID.randomUUID() + "_" + sanitizedFileName;

        // Build the storage path inside the bucket
        String storagePath = (folder != null && !folder.isBlank())
                ? folder.strip() + "/" + storedFileName
                : storedFileName;

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        doUpload(storagePath, file.getBytes(), contentType);

        String publicUrl = buildPublicUrl(storagePath);

        logger.info("Upload Successfully: path={}, url={}", storagePath, publicUrl);

        return FileUploadResponse.builder()
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .contentType(contentType)
                .fileSize(file.getSize())
                .build();
    }

    @Override
    public String deleteFile(String storagePath) {
        String deleteUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                supabaseBucket,
                storagePath
        );

        try {
            webClient.delete()
                     .uri(deleteUrl)
                     .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                     .header("apikey", supabaseKey)
                     .retrieve()
                     .bodyToMono(String.class)
                     .block();

            logger.info("Delete Successfully: {}", storagePath);
            return storagePath;

        } catch (WebClientResponseException e) {
            logger.error("Delete Fail: path={}, status={}, body={}",
                    storagePath, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private void doUpload(String storagePath, byte[] fileBytes, String contentType) {
        String uploadUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                supabaseBucket,
                storagePath
        );

        logger.info("Uploading on Supabase: {}", uploadUrl);

        try {
            webClient.post()
                     .uri(uploadUrl)
                     .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                     .header("apikey", supabaseKey)
                     .header("x-upsert", "false")
                     .header(HttpHeaders.CONTENT_TYPE, contentType)
                     .bodyValue(fileBytes)
                     .retrieve()
                     .bodyToMono(String.class)
                     .block();

        } catch (WebClientResponseException e) {
            logger.error("Upload Fail: path={}, status={}, body={}",
                    storagePath, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String buildPublicUrl(String storagePath) {
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8)
                                       .replace("+", "%20")
                                       .replace("%2F", "/"); // keep the slash character as-is

        return String.format(
                "%s/storage/v1/object/public/%s/%s",
                supabaseUrl,
                supabaseBucket,
                encodedPath
        );
    }
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        String sanitized = fileName.replaceAll("\\s+", "_")
                                   .replaceAll("[^a-zA-Z0-9._-]", "");
        return sanitized.isBlank() ? "file" : sanitized;
    }
}