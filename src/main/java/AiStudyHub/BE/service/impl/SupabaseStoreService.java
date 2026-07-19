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
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String folder) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String storedFileName = UUID.randomUUID() + "_" + sanitizedFileName;

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
    public String deleteFile(String fileUrlPath) {
        String storagePath = extractStoragePath(fileUrlPath);

        String deleteUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                supabaseBucket,
                storagePath);

        try {
            logger.info("Deleting Supabase file: path={}, url={}", storagePath, deleteUrl);

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
        String storagePath = extractStoragePath(fileUrlPath);

        String downloadUrl = String.format(
                "%s/storage/v1/object/authenticated/%s/%s",
                supabaseUrl,
                supabaseBucket,
                URLEncoder.encode(storagePath, StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        .replace("%2F", "/"));

        try {
            logger.info("Downloading Supabase file: path={}, url={}", storagePath, downloadUrl);

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

            String publicUrl = buildPublicUrl(storagePath);

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

    // -----------------------------------------------------------------------------
    private boolean doUpload(String storagePath, byte[] fileBytes, String contentType) {
        String uploadUrl = String.format(
                "%s/storage/v1/object/%s/%s",
                supabaseUrl,
                supabaseBucket,
                storagePath);

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
        return true;
    }

    private String buildPublicUrl(String storagePath) {
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/"); // keep the slash character as-is

        return String.format(
                "%s/storage/v1/object/public/%s/%s",
                supabaseUrl,
                supabaseBucket,
                encodedPath);
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

        String publicMarker = "/storage/v1/object/public/" + supabaseBucket + "/";
        String privateMarker = "/storage/v1/object/" + supabaseBucket + "/";
        String signedMarker = "/storage/v1/object/sign/" + supabaseBucket + "/";

        String path;

        if (value.contains(publicMarker)) {
            path = value.substring(value.indexOf(publicMarker) + publicMarker.length());
        } else if (value.contains(privateMarker)) {
            path = value.substring(value.indexOf(privateMarker) + privateMarker.length());
        } else if (value.contains(signedMarker)) {
            path = value.substring(value.indexOf(signedMarker) + signedMarker.length());
        } else {
            path = value;
        }

        path = removeQueryString(path);

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

    private String removeQueryString(String path) {
        int queryIndex = path.indexOf("?");

        if (queryIndex != -1) {
            return path.substring(0, queryIndex);
        }

        return path;
    }

}
