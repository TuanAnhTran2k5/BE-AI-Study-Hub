package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.SubjectType;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.exception.ResourceNotFoundException;
import AiStudyHub.BE.mapper.SubjectMapper;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.service.impl.ISubject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectService implements ISubject {

    SubjectRepo subjectRepo;
    SemesterRepo semesterRepo;
    ComboSubjectRepo comboSubjectRepo;
    SubjectMapper subjectMapper;

    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepo.findByIsDeletedFalse().stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getSubjectById(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        ComboSubject combo = request.getComboId() != null
                ? comboSubjectRepo.findById(request.getComboId()).orElse(null) : null;

        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .description(request.getDescription())
                .subjectType(request.getSubjectType())
                .semester(semester)
                .comboSubject(combo)
                .build();
        subject = subjectRepo.save(subject);
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        ComboSubject combo = request.getComboId() != null
                ? comboSubjectRepo.findById(request.getComboId()).orElse(null) : null;

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        subject.setSubjectType(request.getSubjectType());
        subject.setSemester(semester);          // môn có thể đổi kỳ
        subject.setComboSubject(combo);         // môn có thể đổi combo
        subject = subjectRepo.save(subject);
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public DeleteResponse deleteSubject(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (subject.getSubjectType() == SubjectType.COMBO) {
            subject.setIsDeleted(true);
            subjectRepo.save(subject);
            return subjectMapper.toDeleteResponse(subject, "Combo Subject soft-deleted successfully", LocalDateTime.now());
        } else {
            subjectRepo.delete(subject);
            return subjectMapper.toDeleteResponse(subject, "Core Subject hard-deleted successfully", LocalDateTime.now());
        }
    }

    // --- Public reads (preserve existing behavior: no isDeleted filter) ---

    @Override
    public List<SubjectResponse> getSubjectsBySemester(Long semesterId) {
        return subjectRepo.findBySemesterSemesterId(semesterId).stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId) {
        List<Subject> baseSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectIsNull(semesterId);
        List<Subject> comboSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectComboId(semesterId, comboId);
        return Stream.concat(baseSubjects.stream(), comboSubjects.stream())
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> searchSubjects(String keyword) {
        return subjectRepo.findBySubjectNameContainingIgnoreCase(keyword).stream()
                .filter(subject -> !subject.getIsDeleted())
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }
}
