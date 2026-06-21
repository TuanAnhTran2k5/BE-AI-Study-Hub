package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.service.impl.IComboSubject;
import AiStudyHub.BE.service.impl.ISemester;
import AiStudyHub.BE.service.impl.ISubject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/curriculum")
@RequiredArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminCurriculumController {

    ISemester semesterService;
    ISubject subjectService;
    IComboSubject comboSubjectService;

    // --- Semesters ---
    @GetMapping("/semesters")
    public ResponseEntity<APIResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(APIResponse.response(200, "Get all semesters successfully", semesterService.getAllSemesters()));
    }

    @GetMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get semester successfully", semesterService.getSemesterById(id)));
    }

    @PostMapping("/semesters")
    public ResponseEntity<APIResponse<SemesterResponse>> createSemester(@RequestBody SemesterRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create semester successfully", semesterService.createSemester(request)));
    }

    @PutMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> updateSemester(@PathVariable Long id, @RequestBody SemesterRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update semester successfully", semesterService.updateSemester(id, request)));
    }

    @DeleteMapping("/semesters/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSemester(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete semester successfully", semesterService.deleteSemester(id)));
    }

    // --- Subjects ---
    @GetMapping("/subjects")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getAllSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search subjects successfully", subjectService.searchSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all subjects successfully", subjectService.getAllSubjects()));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get subject successfully", subjectService.getSubjectById(id)));
    }

    @PostMapping("/subjects")
    public ResponseEntity<APIResponse<SubjectResponse>> createSubject(@RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create subject successfully", subjectService.createSubject(request)));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> updateSubject(@PathVariable Long id, @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update subject successfully", subjectService.updateSubject(id, request)));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete subject successfully", subjectService.deleteSubject(id)));
    }

    // --- ComboSubjects ---
    @GetMapping("/combos")
    public ResponseEntity<APIResponse<List<ComboSubjectResponse>>> getAllComboSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search combo subjects successfully", comboSubjectService.searchComboSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all combo subjects successfully", comboSubjectService.getAllComboSubjects()));
    }

    @GetMapping("/combos/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> getComboSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get combo subject successfully", comboSubjectService.getComboSubjectById(id)));
    }

    @PostMapping("/combos")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> createComboSubject(@RequestBody ComboSubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create combo subject and its subjects successfully", comboSubjectService.createComboSubject(request)));
    }

    @PutMapping("/combos/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> updateComboSubject(@PathVariable Long id, @RequestBody ComboSubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update combo subject successfully. Lời khuyên: Vui lòng kiểm tra và cập nhật các môn học thuộc combo này nếu cần thiết.", comboSubjectService.updateComboSubject(id, request)));
    }

    @DeleteMapping("/combos/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteComboSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete combo subject and its subjects successfully", comboSubjectService.deleteComboSubject(id)));
    }
}
