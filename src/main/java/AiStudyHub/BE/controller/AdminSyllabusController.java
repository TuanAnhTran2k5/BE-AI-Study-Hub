package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.SyllabusUpdateRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.dto.Response.SyllabusHistoryResponse;
import AiStudyHub.BE.dto.Response.SyllabusResponse;
import AiStudyHub.BE.entity.SubjectSyllabus;
import AiStudyHub.BE.entity.SubjectSyllabusHistory;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.SubjectSyllabusRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.ISyllabusService;
import AiStudyHub.BE.service.ISupabaseStorage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/curriculum/syllabus")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminSyllabusController {

    ISyllabusService syllabusService;
    ISupabaseStorage supabaseStorage;
    SubjectSyllabusRepo subjectSyllabusRepo;

    @PostMapping(value = "/upload/{subjectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<SyllabusResponse>> uploadSyllabus(
            @PathVariable Long subjectId,
            @RequestParam("file") MultipartFile file) {
        
        User admin = SecurityUtils.getCurrentUser();
        String adminUsername = admin.getEmail() != null ? admin.getEmail() : admin.getFullName();

        try {
            // 1. Upload PDF to Supabase Storage
            FileUploadResponse uploadResponse = supabaseStorage.uploadFile(file, "syllabus");
            
            // 2. Initialize syllabus record and trigger async parsing
            SubjectSyllabus syllabus = syllabusService.initSyllabusUpload(
                    subjectId, 
                    uploadResponse.getPublicUrl(), 
                    file.getBytes(), 
                    adminUsername
            );

            return ResponseEntity.status(201).body(
                    APIResponse.response(201, "Syllabus uploaded and parsing started in the background", toResponse(syllabus))
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    APIResponse.response(500, "Failed to upload and parse syllabus: " + e.getMessage(), null)
            );
        }
    }

    @PutMapping("/update/{subjectId}")
    public ResponseEntity<APIResponse<SyllabusResponse>> updateSyllabus(
            @PathVariable Long subjectId,
            @RequestBody SyllabusUpdateRequest request) {
        
        User admin = SecurityUtils.getCurrentUser();
        String adminUsername = admin.getEmail() != null ? admin.getEmail() : admin.getFullName();

        SubjectSyllabus syllabus = syllabusService.updateSyllabus(
                subjectId, 
                request.getJsonContent(), 
                adminUsername, 
                request.getReason() != null ? request.getReason() : "Chỉnh sửa trực tiếp trên Admin Dashboard"
        );

        return ResponseEntity.ok(
                APIResponse.response(200, "Syllabus updated successfully and AI Sync triggered", toResponse(syllabus))
        );
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<APIResponse<SyllabusResponse>> getSyllabus(@PathVariable Long subjectId) {
        SubjectSyllabus syllabus = subjectSyllabusRepo.findBySubjectSubjectId(subjectId).orElse(null);
        if (syllabus == null || Boolean.TRUE.equals(syllabus.getSubject().getIsDeleted())) {
            return ResponseEntity.status(404).body(
                    APIResponse.response(404, "No syllabus found for this subject", null)
            );
        }
        return ResponseEntity.ok(
                APIResponse.response(200, "Retrieve syllabus successfully", toResponse(syllabus))
        );
    }

    @GetMapping("/history/{subjectId}")
    public ResponseEntity<APIResponse<List<SyllabusHistoryResponse>>> getSyllabusHistory(@PathVariable Long subjectId) {
        List<SubjectSyllabusHistory> historyList = syllabusService.getHistory(subjectId);
        List<SyllabusHistoryResponse> responseList = historyList.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                APIResponse.response(200, "Retrieve syllabus history successfully", responseList)
        );
    }

    @PostMapping("/rollback/{subjectId}/{historyId}")
    public ResponseEntity<APIResponse<SyllabusResponse>> rollbackSyllabus(
            @PathVariable Long subjectId,
            @PathVariable Long historyId) {
        
        User admin = SecurityUtils.getCurrentUser();
        String adminUsername = admin.getEmail() != null ? admin.getEmail() : admin.getFullName();

        SubjectSyllabus syllabus = syllabusService.rollback(subjectId, historyId, adminUsername);

        return ResponseEntity.ok(
                APIResponse.response(200, "Syllabus rolled back successfully and AI Sync triggered", toResponse(syllabus))
        );
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<APIResponse<Void>> deleteSyllabus(@PathVariable Long subjectId) {
        syllabusService.deleteSyllabus(subjectId);
        return ResponseEntity.ok(
                APIResponse.response(200, "Syllabus deleted successfully from database and vector store", null)
        );
    }

    // ==========================================
    //            MAPPER HELPERS
    // ==========================================

    private SyllabusResponse toResponse(SubjectSyllabus syllabus) {
        if (syllabus == null) return null;
        return SyllabusResponse.builder()
                .id(syllabus.getId())
                .subjectId(syllabus.getSubject().getSubjectId())
                .subjectCode(syllabus.getSubject().getSubjectCode())
                .subjectName(syllabus.getSubject().getSubjectName())
                .pdfUrl(syllabus.getPdfUrl())
                .jsonContent(syllabus.getJsonContent())
                .syncStatus(syllabus.getSyncStatus())
                .parserVersion(syllabus.getParserVersion())
                .embeddingModel(syllabus.getEmbeddingModel())
                .embeddingVersion(syllabus.getEmbeddingVersion())
                .updatedAt(syllabus.getUpdatedAt())
                .build();
    }

    private SyllabusHistoryResponse toHistoryResponse(SubjectSyllabusHistory history) {
        if (history == null) return null;
        return SyllabusHistoryResponse.builder()
                .id(history.getId())
                .subjectSyllabusId(history.getSubjectSyllabus().getId())
                .pdfUrl(history.getPdfUrl())
                .jsonContent(history.getJsonContent())
                .version(history.getVersion())
                .updatedBy(history.getUpdatedBy())
                .updatedReason(history.getUpdatedReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
