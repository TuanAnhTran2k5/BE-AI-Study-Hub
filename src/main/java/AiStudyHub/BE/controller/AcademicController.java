package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.service.ICurriculum;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/academic")
@CrossOrigin("*")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicController {

    ICurriculum curriculumService;

    @GetMapping("/semesters")
    public ResponseEntity<APIResponse<?>> getAllSemesters() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get semesters successfully", curriculumService.getAllSemesters())
        );
    }

    @GetMapping("/combos")
    public ResponseEntity<APIResponse<?>> getAllCombos() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get combos successfully", curriculumService.getAllComboSubjects())
        );
    }

    @GetMapping("/subjects/semester/{semesterId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemester(
            @PathVariable Long semesterId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester successfully", curriculumService.getSubjectsBySemester(semesterId))
        );
    }

    @GetMapping("/subjects/semester/{semesterId}/combo/{comboId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemesterAndCombo(
            @PathVariable Long semesterId,
            @PathVariable Long comboId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester and combo successfully", curriculumService.getSubjectsBySemesterAndCombo(semesterId, comboId))
        );
    }
}
