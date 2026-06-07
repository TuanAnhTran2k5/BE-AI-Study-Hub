package AiStudyHub.BE.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SupabaseStoreService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStoreService.class);

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String supabaseBucket;

    public SupabaseStoreService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String uploadFile(String fileName, byte[] fileBytes, String contentType) {
        String sanitizedFileName = sanitizeFileName(fileName);
        String uniqueFileName = UUID.randomUUID() + "_" + sanitizedFileName;

        try {
            String uploadUrl = String.format(
                    "%s/storage/v1/object/%s/%s",
                    supabaseUrl,
                    supabaseBucket,
                    uniqueFileName
            );

            logger.info("Uploading file to Supabase: {}", uniqueFileName);

            webClient.post()
                     .uri(uploadUrl)
                     .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                     .header("apikey", supabaseKey)
                     .header("x-upsert", "false")
                     .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : "application/octet-stream")
                     .bodyValue(fileBytes)
                     .retrieve()
                     .bodyToMono(String.class)
                     .block();

            String publicUrl = generatePublicUrl(uniqueFileName);

            logger.info("Upload successful: {}", publicUrl);

            return publicUrl;

        } catch (WebClientException e) {
            logger.error("Failed to upload file {}", fileName, e);
            throw new RuntimeException("Upload Supabase failed: " + e.getMessage());
        }
    }

    private String generatePublicUrl(String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                                           .replace("+", "%20");

        return String.format(
                "%s/storage/v1/object/public/%s/%s",
                supabaseUrl,
                supabaseBucket,
                encodedFileName
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }

        String sanitized = fileName.replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "");

        if (sanitized.isBlank()) {
            return "file";
        }

        return sanitized;
    }
}