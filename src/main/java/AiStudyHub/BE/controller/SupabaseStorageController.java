package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.service.SupabaseStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@SecurityRequirement(name = "api")
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
    }
}