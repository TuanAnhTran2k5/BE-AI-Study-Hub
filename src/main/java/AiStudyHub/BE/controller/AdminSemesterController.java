package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.service.ISemesterService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/curriculum/semesters")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AdminSemesterController {

    ISemesterService semesterService;

    @GetMapping
    public ResponseEntity<APIResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(APIResponse.response(200, "Get all semesters successfully", semesterService.getAllSemesters()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Get semester successfully", semesterService.getSemesterById(id)));
    }

    @PostMapping
    public ResponseEntity<APIResponse<SemesterResponse>> createSemester(@RequestBody SemesterRequest request) {
        return ResponseEntity.status(201).body(APIResponse.response(201, "Create semester successfully", semesterService.createSemester(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<SemesterResponse>> updateSemester(@PathVariable Long id, @RequestBody SemesterRequest request) {
        return ResponseEntity.ok(APIResponse.response(200, "Update semester successfully", semesterService.updateSemester(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<DeleteResponse>> deleteSemester(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.response(200, "Delete semester successfully", semesterService.deleteSemester(id)));
    }
}
