package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.service.ICurriculum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/curriculum")
@RequiredArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminCurriculumController {

    ICurriculum curriculumService;

    // --- Semesters ---
    @GetMapping("/semesters")
    public ResponseEntity<APIResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(APIResponse.response(200, "Get all semesters successfully", curriculumService.getAllSemesters()));
    }

    @GetMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get semester successfully", curriculumService.getSemesterById(id)));
    }

    @PostMapping("/semesters")
    public ResponseEntity<APIResponse<SemesterResponse>> createSemester(@RequestBody SemesterRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create semester successfully", curriculumService.createSemester(request)));
    }

    @PutMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> updateSemester(@PathVariable Long id, @RequestBody SemesterRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update semester successfully", curriculumService.updateSemester(id, request)));
    }

    @DeleteMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSemester(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete semester successfully", curriculumService.deleteSemester(id)));
    }

    // --- Subjects ---
    @GetMapping("/subjects")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getAllSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search subjects successfully", curriculumService.searchSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all subjects successfully", curriculumService.getAllSubjects()));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get subject successfully", curriculumService.getSubjectById(id)));
    }

    @PostMapping("/subjects")
    public ResponseEntity<APIResponse<SubjectResponse>> createSubject(@RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create subject successfully", curriculumService.createSubject(request)));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> updateSubject(@PathVariable Long id, @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update subject successfully", curriculumService.updateSubject(id, request)));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete subject successfully", curriculumService.deleteSubject(id)));
    }

    // --- ComboSubjects ---
    @GetMapping("/combos")
    public ResponseEntity<APIResponse<List<ComboSubjectResponse>>> getAllComboSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search combo subjects successfully", curriculumService.searchComboSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all combo subjects successfully", curriculumService.getAllComboSubjects()));
    }

    @GetMapping("/combos/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> getComboSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get combo subject successfully", curriculumService.getComboSubjectById(id)));
    }

    @PostMapping("/combos")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> createComboSubject(@RequestBody ComboSubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create combo subject and its subjects successfully", curriculumService.createComboSubject(request)));
    }

    @PutMapping("/combos/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> updateComboSubject(@PathVariable Long id, @RequestBody ComboSubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update combo subject successfully. Advice: Please check and update the subjects belonging to this combo if necessary.", curriculumService.updateComboSubject(id, request)));
    }

    @DeleteMapping("/combos/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteComboSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete combo subject and its subjects successfully", curriculumService.deleteComboSubject(id)));
    }
}
