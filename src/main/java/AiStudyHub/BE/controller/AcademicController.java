package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.service.impl.IComboSubject;
import AiStudyHub.BE.service.impl.ISemester;
import AiStudyHub.BE.service.impl.ISubject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/academic")
@SecurityRequirement(name = "api")
@CrossOrigin("*")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicController {

    ISemester semesterService;
    ISubject subjectService;
    IComboSubject comboSubjectService;

    @GetMapping("/semesters")
    public ResponseEntity<APIResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get semesters successfully", semesterService.getAllSemesters())
        );
    }

    @GetMapping("/combos")
    public ResponseEntity<APIResponse<List<ComboSubjectResponse>>> getAllCombos() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get combos successfully", comboSubjectService.getAllComboSubjects())
        );
    }

    @GetMapping("/combos/search")
    public ResponseEntity<APIResponse<List<ComboSubjectResponse>>> searchCombos(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Search combos successfully", comboSubjectService.searchComboSubjects(keyword))
        );
    }

    @GetMapping("/subjects/semester/{semesterId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemester(
            @PathVariable Long semesterId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester successfully",
                        subjectService.getSubjectsBySemester(semesterId))
        );
    }

    @GetMapping("/subjects/search")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> searchSubjects(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Search subjects successfully",
                        subjectService.searchSubjects(keyword))
        );
    }

    @GetMapping("/subjects/semester/{semesterId}/combo/{comboId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemesterAndCombo(
            @PathVariable Long semesterId,
            @PathVariable Long comboId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester and combo successfully",
                        subjectService.getSubjectsBySemesterAndCombo(semesterId, comboId))
        );
    }
}
