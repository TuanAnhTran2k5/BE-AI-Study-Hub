package AiStudyHub.BE.controller;

import AiStudyHub.BE.dto.Response.APIResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/academic")
@CrossOrigin("*")
public class AcademicController {

    @Autowired
    private SemesterRepo semesterRepo;

    @Autowired
    private SubjectRepo subjectRepo;

    @Autowired
    private ComboSubjectRepo comboSubjectRepo;

    @GetMapping("/semesters")
    public ResponseEntity<APIResponse<?>> getAllSemesters() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get semesters successfully", semesterRepo.findAll())
        );
    }

    @GetMapping("/combos")
    public ResponseEntity<APIResponse<?>> getAllCombos() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get combos successfully", comboSubjectRepo.findAll())
        );
    }

    @GetMapping("/subjects/semester/{semesterId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemester(
            @PathVariable Long semesterId
    ) {
        List<SubjectResponse> result = subjectRepo.findBySemester_SemesterId(semesterId)
                                                  .stream()
                                                  .map(this::toSubjectResponse)
                                                  .toList();

        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester successfully", result)
        );
    }

    @GetMapping("/subjects/semester/{semesterId}/combo/{comboId}")
    public ResponseEntity<APIResponse<List<SubjectResponse>>> getSubjectsBySemesterAndCombo(
            @PathVariable Long semesterId,
            @PathVariable Long comboId
    ) {
        List<Subject> baseSubjects = subjectRepo.findBySemester_SemesterIdAndComboSubjectIsNull(semesterId);
        List<Subject> comboSubjects = subjectRepo.findBySemester_SemesterIdAndComboSubject_ComboId(semesterId, comboId);
        
        List<SubjectResponse> result = java.util.stream.Stream.concat(baseSubjects.stream(), comboSubjects.stream())
                                                  .map(this::toSubjectResponse)
                                                  .toList();

        return ResponseEntity.ok(
                APIResponse.response(200, "Get subjects by semester and combo successfully", result)
        );
    }

    private SubjectResponse toSubjectResponse(Subject subject) {
        return SubjectResponse.builder()
                              .subjectId(subject.getSubjectId())
                              .subjectCode(subject.getSubjectCode())
                              .subjectName(subject.getSubjectName())
                              .description(subject.getDescription())
                              .subjectType(subject.getSubjectType())

                              .semesterId(subject.getSemester().getSemesterId())
                              .semesterNo(subject.getSemester().getSemesterNo())

                              .comboId(subject.getComboSubject() == null ? null : subject.getComboSubject().getComboId())
                              .comboCode(subject.getComboSubject() == null ? null : subject.getComboSubject().getComboCode())
                              .comboName(subject.getComboSubject() == null ? null : subject.getComboSubject().getComboName())
                              .build();
    }
}