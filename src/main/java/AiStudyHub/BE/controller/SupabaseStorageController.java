package AiStudyHub.BE.controller;

<<<<<<< Updated upstream
import AiStudyHub.BE.service.SupabaseStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
=======
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import AiStudyHub.BE.dto.Response.APIResponse;
import org.springframework.http.ResponseEntity;
>>>>>>> Stashed changes

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@SecurityRequirement(name = "api")
public class SupabaseStorageController {

<<<<<<< Updated upstream
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
=======
    @Operation(
            summary = "Thông tin Storage API",
            description = "Upload tài liệu vui lòng dùng endpoint: `POST /api/user/document/upload`"
    )
    @GetMapping("/info")
    public ResponseEntity<APIResponse<Map<String, String>>> info() {
        Map<String, String> data = Map.of(
                "message", "Vui lòng dùng POST /api/user/document/upload để upload tài liệu",
                "uploadEndpoint", "/api/user/document/upload"
        );
        
        return ResponseEntity.status(200)
                .body(
                        APIResponse.response(
                                200, "Thông tin API Storage", data
                        ));
>>>>>>> Stashed changes
    }
}