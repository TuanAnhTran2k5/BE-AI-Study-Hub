package AiStudyHub.BE.controller;

import AiStudyHub.BE.service.SupabaseStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class SupabaseStorageController {

    private final SupabaseStoreService supabaseStoreService;

    public SupabaseStorageController(SupabaseStoreService supabaseStoreService) {
        this.supabaseStoreService = supabaseStoreService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        String fileUrl = supabaseStoreService.uploadFile(
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "File uploaded successfully",
                        "fileUrl", fileUrl
                )
        );
    }
}