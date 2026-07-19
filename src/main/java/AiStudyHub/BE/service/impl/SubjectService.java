package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.SubjectType;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.SubjectMapper;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.service.ISubjectService;
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
public class SubjectService implements ISubjectService {

    SemesterRepo semesterRepo;
    SubjectRepo subjectRepo;
    ComboSubjectRepo comboSubjectRepo;
    SubjectMapper subjectMapper;

    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepo.findAll().stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getSubjectById(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Subject not found"));
        if (Boolean.TRUE.equals(subject.getIsDeleted())) {
            throw new GlobalException(404, "Subject not found");
        }
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        java.util.Optional<Subject> existingOpt = subjectRepo.findBySubjectCode(request.getSubjectCode());
        if (existingOpt.isPresent()) {
            Subject existing = existingOpt.get();
            if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                throw new GlobalException(400, "Subject code already exists and is active");
            } else {
                throw new GlobalException(400, "Subject code already exists in a deleted subject. Please restore it.");
            }
        }

        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new GlobalException(404, "Semester not found"));
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
                .orElseThrow(() -> new GlobalException(404, "Subject not found"));
        if (Boolean.TRUE.equals(subject.getIsDeleted())) {
            throw new GlobalException(404, "Subject not found");
        }

        if (!subject.getSubjectCode().equalsIgnoreCase(request.getSubjectCode())) {
            java.util.Optional<Subject> existingOpt = subjectRepo.findBySubjectCode(request.getSubjectCode());
            if (existingOpt.isPresent()) {
                Subject existing = existingOpt.get();
                if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                    throw new GlobalException(400, "Subject code already exists and is active");
                } else {
                    throw new GlobalException(400, "Subject code already exists in a deleted subject. Please restore it.");
                }
            }
        }

        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new GlobalException(404, "Semester not found"));
        ComboSubject combo = request.getComboId() != null
                ? comboSubjectRepo.findById(request.getComboId()).orElse(null) : null;

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        subject.setSubjectType(request.getSubjectType());
        subject.setSemester(semester);
        subject.setComboSubject(combo);
        subject = subjectRepo.save(subject);
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public DeleteResponse deleteSubject(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Subject not found"));
        if (Boolean.TRUE.equals(subject.getIsDeleted())) {
            throw new GlobalException(404, "Subject not found");
        }
        subject.setIsDeleted(true);
        subject = subjectRepo.save(subject);
        return subjectMapper.toDeleteResponse(subject, "Subject soft-deleted successfully", LocalDateTime.now());
    }

    @Override
    public List<SubjectResponse> getSubjectsBySemester(Long semesterId) {
        return subjectRepo.findBySemesterSemesterIdAndIsDeletedFalse(semesterId).stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId) {
        List<Subject> baseSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectIsNullAndIsDeletedFalse(semesterId);
        List<Subject> comboSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectComboIdAndIsDeletedFalse(semesterId, comboId);
        return Stream.concat(baseSubjects.stream(), comboSubjects.stream())
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> searchSubjects(String keyword) {
        return subjectRepo.findBySubjectNameContainingIgnoreCaseAndIsDeletedFalse(keyword).stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectResponse restoreSubject(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new GlobalException(404, "Subject not found"));
        if (!Boolean.TRUE.equals(subject.getIsDeleted())) {
            throw new GlobalException(400, "Subject is already active");
        }

        java.util.Optional<Subject> activeOpt = subjectRepo.findBySubjectCode(subject.getSubjectCode());
        if (activeOpt.isPresent() && !Boolean.TRUE.equals(activeOpt.get().getIsDeleted()) && !activeOpt.get().getSubjectId().equals(subjectId)) {
            throw new GlobalException(400, "Cannot restore because subject code '" + subject.getSubjectCode() + "' is already in use by another active subject");
        }

        if (subject.getComboSubject() != null && Boolean.TRUE.equals(subject.getComboSubject().getIsDeleted())) {
            subject.getComboSubject().setIsDeleted(false);
            comboSubjectRepo.save(subject.getComboSubject());
        }

        subject.setIsDeleted(false);
        subject = subjectRepo.save(subject);
        return subjectMapper.toSubjectResponse(subject);
    }
}
