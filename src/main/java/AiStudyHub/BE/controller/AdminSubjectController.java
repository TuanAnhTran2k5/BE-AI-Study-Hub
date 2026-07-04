package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.service.ISubjectService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/curriculum/subjects")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminSubjectController {

    ISubjectService subjectService;

    @GetMapping
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getAllSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search subjects successfully", subjectService.searchSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all subjects successfully", subjectService.getAllSubjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get subject successfully", subjectService.getSubjectById(id)));
    }

    @PostMapping
    public ResponseEntity<APIResponse<SubjectResponse>> createSubject(@RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create subject successfully", subjectService.createSubject(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<SubjectResponse>> updateSubject(@PathVariable Long id, @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update subject successfully", subjectService.updateSubject(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete subject successfully", subjectService.deleteSubject(id)));
    }
}
