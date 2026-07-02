package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.service.IComboSubjectService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/curriculum/combos")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminComboSubjectController {

    IComboSubjectService comboSubjectService;

    @GetMapping
    public ResponseEntity<APIResponse<List<ComboSubjectResponse>>> getAllComboSubjects(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(APIResponse.response(200, "Search combo subjects successfully", comboSubjectService.searchComboSubjects(keyword.trim())));
        }
        return ResponseEntity.ok(APIResponse.response(200, "Get all combo subjects successfully", comboSubjectService.getAllComboSubjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> getComboSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get combo subject successfully", comboSubjectService.getComboSubjectById(id)));
    }

    @PostMapping
    public ResponseEntity<APIResponse<ComboSubjectResponse>> createComboSubject(@RequestBody ComboSubjectRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create combo subject and its subjects successfully", comboSubjectService.createComboSubject(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<ComboSubjectResponse>> updateComboSubject(@PathVariable Long id, @RequestBody ComboSubjectRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update combo subject successfully. Advice: Please check and update the subjects belonging to this combo if necessary.", comboSubjectService.updateComboSubject(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteComboSubject(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete combo subject and its subjects successfully", comboSubjectService.deleteComboSubject(id)));
    }
}
