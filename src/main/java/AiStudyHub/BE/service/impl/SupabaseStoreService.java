package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.ISupabaseStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class SupabaseStoreService implements ISupabaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStoreService.class);

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseKey;

    /** Bucket mặc định dùng cho Document uploads */
    @Value("${supabase.storage.bucket}")
    private String supabaseBucket;

    /** Bucket dành riêng cho Avatar uploads */
    @Value("${supabase.storage.avatar-bucket}")
    private String avatarBucket;

    public SupabaseStoreService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API — Document Bucket (giữ nguyên không thay đổi)
    // ─────────────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) throws Exception {
        return uploadFileToBucket(file, folder, supabaseBucket);
    }

    @Override
    public String deleteFile(String fileUrlPath) {
        String storagePath = extractStoragePath(fileUrlPath);
        // Xác định bucket từ URL
        String bucket = detectBucketFromUrl(fileUrlPath);

        String deleteUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                bucket,
                storagePath);

        try {
            logger.info("Deleting Supabase file: path={}, bucket={}, url={}", storagePath, bucket, deleteUrl);

            webClient.delete()
                    .uri(deleteUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Delete Successfully: path={}", storagePath);

            return "Deleted file successfully: " + storagePath;

        } catch (WebClientResponseException e) {
            logger.error("Delete Fail: path={}, url={}, status={}, body={}",
                    storagePath,
                    deleteUrl,
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            // Handle the case where the file is already deleted or missing
            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            if (statusCode == 404 ||
                    (statusCode == 400 && responseBody != null
                            && (responseBody.contains("not_found") || responseBody.contains("Object not found")))) {
                logger.warn("File already missing from Supabase storage, treating deletion as successful: path={}",
                        storagePath);
                return "Deleted file successfully (file was already missing): " + storagePath;
            }

            throw new GlobalException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public byte[] downloadFile(String fileUrlPath) {
        // If the stored URL is already a public URL, download directly from it.
        // This avoids rebuilding an authenticated URL which may fail DNS in some environments.
        boolean isPublicUrl = fileUrlPath != null && fileUrlPath.contains("/storage/v1/object/public/");

        if (isPublicUrl) {
            try {
                logger.info("Downloading Supabase file via public URL: {}", fileUrlPath);
                return webClient.get()
                        .uri(URI.create(fileUrlPath))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();
            } catch (WebClientResponseException e) {
                logger.warn("Public URL download failed (status={}, body={}), retrying with authenticated URL...",
                        e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                logger.warn("Public URL download failed ({}), retrying with authenticated URL...", e.getMessage());
            }
        }

        // Fallback: build authenticated URL
        String bucket = detectBucketFromUrl(fileUrlPath);
        String storagePath = extractStoragePath(fileUrlPath);
        String downloadUrl = String.format(
                "%s/storage/v1/object/authenticated/%s/%s",
                supabaseUrl,
                bucket,
                URLEncoder.encode(storagePath, StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        .replace("%2F", "/"));

        try {
            logger.info("Downloading Supabase file via authenticated URL: path={}, url={}", storagePath, downloadUrl);

            return webClient.get()
                    .uri(URI.create(downloadUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

        } catch (WebClientResponseException e) {
            logger.error("Download Fail WCRE: path={}, url={}, status={}, body={}",
                    storagePath,
                    downloadUrl,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e);

            throw new GlobalException(ErrorCode.FILE_DOWNLOAD_FAILED);
        } catch (Exception e) {
            logger.error("Download Fail EX: path={}, url={}, error={}",
                    storagePath,
                    downloadUrl,
                    e.getClass().getName(),
                    e);

            throw new GlobalException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
    public FileUploadResponse uploadBytes(byte[] data, String originalFileName, String folder, String contentType)
            throws Exception {
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String storedFileName = UUID.randomUUID() + "_" + sanitizedFileName;

        String storagePath = (folder != null && !folder.isBlank())
                ? folder.strip() + "/" + storedFileName
                : storedFileName;

        String finalContentType = contentType != null && !contentType.isBlank()
                ? contentType
                : "application/octet-stream";

        String uploadUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                supabaseBucket,
                storagePath);

        try {
            webClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header(HttpHeaders.CONTENT_TYPE, finalContentType)
                    .bodyValue(data)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String publicUrl = buildPublicUrl(storagePath, supabaseBucket);

            logger.info("Upload bytes successfully: path={}, url={}", storagePath, publicUrl);

            return FileUploadResponse.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .publicUrl(publicUrl)
                    .contentType(finalContentType)
                    .fileSize((long) data.length)
                    .build();

        } catch (WebClientResponseException e) {
            logger.error("Upload bytes fail: path={}, url={}, status={}, body={}",
                    storagePath,
                    uploadUrl,
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API — Multi-bucket support (dùng cho Avatar bucket)
    // ─────────────────────────────────────────────────────────────────
    @Override
    public FileUploadResponse uploadFileToBucket(MultipartFile file, String folder, String bucket) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String storedFileName = UUID.randomUUID() + "_" + sanitizedFileName;

        String storagePath = (folder != null && !folder.isBlank())
                ? folder.strip() + "/" + storedFileName
                : storedFileName;

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        doUpload(storagePath, file.getBytes(), contentType, bucket);

        String publicUrl = buildPublicUrl(storagePath, bucket);

        logger.info("Upload to bucket '{}' Successfully: path={}, url={}", bucket, storagePath, publicUrl);

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
    public FileUploadResponse downloadAndUploadToBucket(String imageUrl, String folder, String bucket) throws Exception {
        logger.info("Downloading external image from URL: {}", imageUrl);

        byte[] imageBytes;
        String contentType;

        try {
            // Tải ảnh từ URL bên ngoài (không cần auth)
            var response = webClient.get()
                    .uri(URI.create(imageUrl))
                    .retrieve()
                    .toEntity(byte[].class)
                    .block();

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            imageBytes = response.getBody();

            // Lấy content-type từ response header, fallback về image/jpeg
            contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : "image/jpeg";

        } catch (WebClientResponseException e) {
            logger.error("Failed to download external image: url={}, status={}", imageUrl, e.getStatusCode());
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            logger.error("Failed to download external image: url={}, error={}", imageUrl, e.getMessage());
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // Tạo tên file từ extension phù hợp content-type
        String extension = resolveExtension(contentType);
        String storedFileName = UUID.randomUUID() + "_avatar" + extension;

        String storagePath = (folder != null && !folder.isBlank())
                ? folder.strip() + "/" + storedFileName
                : storedFileName;

        doUpload(storagePath, imageBytes, contentType, bucket);

        String publicUrl = buildPublicUrl(storagePath, bucket);

        logger.info("Re-uploaded external image to bucket '{}': path={}, url={}", bucket, storagePath, publicUrl);

        return FileUploadResponse.builder()
                .originalFileName(storedFileName)
                .storedFileName(storedFileName)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .contentType(contentType)
                .fileSize((long) imageBytes.length)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private boolean doUpload(String storagePath, byte[] fileBytes, String contentType, String bucket) {
        String uploadUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                bucket,
                storagePath);

        logger.info("Uploading on Supabase bucket '{}': {}", bucket, uploadUrl);

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
            logger.error("Upload Fail: path={}, bucket={}, status={}, body={}",
                    storagePath, bucket, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return true;
    }

    private String buildPublicUrl(String storagePath, String bucket) {
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/"); // giữ nguyên dấu slash

        return String.format(
                "%s/storage/v1/object/public/%s/%s",
                supabaseUrl,
                bucket,
                encodedPath);
    }

    private String detectBucketFromUrl(String fileUrlPath) {
        if (fileUrlPath == null) return supabaseBucket;
        // Danh sách các bucket đã biết — avatar bucket được kiểm tra trước
        for (String bucket : List.of(avatarBucket, supabaseBucket)) {
            if (fileUrlPath.contains("/" + bucket + "/")) {
                return bucket;
            }
        }
        return supabaseBucket; // fallback về Documents
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        String sanitized = fileName.replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private String extractStoragePath(String fileUrlOrPath) {

        if (fileUrlOrPath == null || fileUrlOrPath.isBlank()) {
            throw new GlobalException(ErrorCode.FILE_DELETE_FAILED);
        }

        String value = fileUrlOrPath.trim();

        // Kiểm tra tất cả các bucket đã biết
        for (String bucket : List.of(avatarBucket, supabaseBucket)) {
            String publicMarker = "/storage/v1/object/public/" + bucket + "/";
            String privateMarker = "/storage/v1/object/" + bucket + "/";
            String signedMarker = "/storage/v1/object/sign/" + bucket + "/";

            if (value.contains(publicMarker)) {
                return decode(removeQueryString(value.substring(value.indexOf(publicMarker) + publicMarker.length())));
            } else if (value.contains(privateMarker)) {
                return decode(removeQueryString(value.substring(value.indexOf(privateMarker) + privateMarker.length())));
            } else if (value.contains(signedMarker)) {
                return decode(removeQueryString(value.substring(value.indexOf(signedMarker) + signedMarker.length())));
            }
        }

        // Nếu không match pattern nào, dùng nguyên giá trị (có thể đã là path)
        String path = removeQueryString(value);
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return decode(path);
    }

    private String decode(String path) {
        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

    private String removeQueryString(String path) {
        int queryIndex = path.indexOf("?");
        return queryIndex != -1 ? path.substring(0, queryIndex) : path;
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) return ".jpg";
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("webp")) return ".webp";
        if (contentType.contains("avif")) return ".avif";
        return ".jpg"; // default jpeg
    }

}
