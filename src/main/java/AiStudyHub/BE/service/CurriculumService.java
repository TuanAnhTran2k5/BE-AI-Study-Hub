package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.exception.ResourceNotFoundException;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.service.impl.ICurriculum;
import AiStudyHub.BE.mapper.CurriculumMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurriculumService implements ICurriculum {

    private final SemesterRepo semesterRepo;
    private final SubjectRepo subjectRepo;
    private final ComboSubjectRepo comboSubjectRepo;
    private final CurriculumMapper curriculumMapper;

    private ComboSubjectResponse mapToComboSubjectResponse(ComboSubject combo) {
        ComboSubjectResponse response = curriculumMapper.toComboSubjectResponse(combo);
        List<SubjectResponse> subjects = subjectRepo.findByComboSubjectComboId(combo.getComboId())
                .stream().map(curriculumMapper::toSubjectResponse).collect(Collectors.toList());
        response.setSubjects(subjects);
        return response;
    }

    // --- Semester ---
    @Override
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepo.findAll().stream().map(curriculumMapper::toSemesterResponse).collect(Collectors.toList());
    }

    @Override
    public SemesterResponse getSemesterById(Long semesterId) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        return curriculumMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        Semester semester = Semester.builder()
                .semesterNo(request.getSemesterNo())
                .description(request.getDescription())
                .build();
        semester = semesterRepo.save(semester);
        return curriculumMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse updateSemester(Long semesterId, SemesterRequest request) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        semester.setSemesterNo(request.getSemesterNo());
        semester.setDescription(request.getDescription());
        semester = semesterRepo.save(semester);
        return curriculumMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public DeleteResponse deleteSemester(Long semesterId) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        semesterRepo.delete(semester);
        return curriculumMapper.toDeleteResponse(semester, java.time.LocalDateTime.now());
    }

    // --- Subject ---
    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepo.findByIsDeletedFalse().stream().map(curriculumMapper::toSubjectResponse).collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getSubjectById(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return curriculumMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        ComboSubject combo = request.getComboId() != null ? 
                comboSubjectRepo.findById(request.getComboId()).orElse(null) : null;

        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .description(request.getDescription())
                .subjectType(request.getSubjectType())
                .semester(semester)
                .comboSubject(combo)
                .build();
        subject = subjectRepo.save(subject);
        return curriculumMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        Semester semester = semesterRepo.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        ComboSubject combo = request.getComboId() != null ? 
                comboSubjectRepo.findById(request.getComboId()).orElse(null) : null;

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        subject.setSubjectType(request.getSubjectType());
        subject.setSemester(semester);
        subject.setComboSubject(combo);
        subject = subjectRepo.save(subject);
        return curriculumMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    public DeleteResponse deleteSubject(Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (subject.getSubjectType() == AiStudyHub.BE.constraint.SubjectType.COMBO) {
            subject.setIsDeleted(true);
            subjectRepo.save(subject);
            return curriculumMapper.toDeleteResponse(subject, "Combo Subject soft-deleted successfully", java.time.LocalDateTime.now());
        } else {
            subjectRepo.delete(subject);
            return curriculumMapper.toDeleteResponse(subject, "Core Subject hard-deleted successfully", java.time.LocalDateTime.now());
        }
    }

    // --- ComboSubject ---
    @Override
    public List<ComboSubjectResponse> getAllComboSubjects() {
        return comboSubjectRepo.findByIsDeletedFalse().stream().map(this::mapToComboSubjectResponse).collect(Collectors.toList());
    }

    @Override
    public ComboSubjectResponse getComboSubjectById(Long comboId) {
        ComboSubject combo = comboSubjectRepo.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("ComboSubject not found"));
        return mapToComboSubjectResponse(combo);
    }

    @Override
    @Transactional
    public ComboSubjectResponse createComboSubject(ComboSubjectRequest request) {
        if (request.getSubjects() == null || request.getSubjects().isEmpty()) {
            throw new IllegalArgumentException("Cannot create a Combo without Subjects");
        }

        ComboSubject combo = ComboSubject.builder()
                .comboCode(request.getComboCode())
                .comboName(request.getComboName())
                .build();
        combo = comboSubjectRepo.save(combo);

        for (SubjectRequest subReq : request.getSubjects()) {
            Semester semester = semesterRepo.findById(subReq.getSemesterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Semester not found: " + subReq.getSemesterId()));
            Subject subject = Subject.builder()
                    .subjectCode(subReq.getSubjectCode())
                    .subjectName(subReq.getSubjectName())
                    .description(subReq.getDescription())
                    .subjectType(subReq.getSubjectType())
                    .semester(semester)
                    .comboSubject(combo)
                    .build();
            subjectRepo.save(subject);
        }

        return mapToComboSubjectResponse(combo);
    }

    @Override
    @Transactional
    public ComboSubjectResponse updateComboSubject(Long comboId, ComboSubjectRequest request) {
        ComboSubject combo = comboSubjectRepo.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("ComboSubject not found"));
        
        combo.setComboCode(request.getComboCode());
        combo.setComboName(request.getComboName());
        combo = comboSubjectRepo.save(combo);

        if (request.getSubjects() != null) {
            // Optional: update subjects logic if admin provided them
            // We could wipe out existing subjects and recreate, or update matching ones.
            // For simplicity and safety, we recreate if provided.
            List<Subject> existingSubjects = subjectRepo.findByComboSubjectComboId(combo.getComboId());
            for (Subject s : existingSubjects) {
                s.setIsDeleted(true);
            }
            subjectRepo.saveAll(existingSubjects);

            for (SubjectRequest subReq : request.getSubjects()) {
                Semester semester = semesterRepo.findById(subReq.getSemesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Semester not found: " + subReq.getSemesterId()));
                Subject subject = Subject.builder()
                        .subjectCode(subReq.getSubjectCode())
                        .subjectName(subReq.getSubjectName())
                        .description(subReq.getDescription())
                        .subjectType(subReq.getSubjectType())
                        .semester(semester)
                        .comboSubject(combo)
                        .build();
                subjectRepo.save(subject);
            }
        }
        return mapToComboSubjectResponse(combo);
    }

    @Override
    @Transactional
    public DeleteResponse deleteComboSubject(Long comboId) {
        ComboSubject combo = comboSubjectRepo.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("ComboSubject not found"));
        
        // Cascade soft-delete subjects
        List<Subject> subjects = subjectRepo.findByComboSubjectComboId(combo.getComboId());
        for (Subject subject : subjects) {
            subject.setIsDeleted(true);
        }
        if (!subjects.isEmpty()) {
            subjectRepo.saveAll(subjects);
        }
        
        combo.setIsDeleted(true);
        comboSubjectRepo.save(combo);
        return curriculumMapper.toDeleteResponse(combo, java.time.LocalDateTime.now());
    }
}
