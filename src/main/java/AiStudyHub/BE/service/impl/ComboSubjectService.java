package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.ComboSubjectMapper;
import AiStudyHub.BE.mapper.SubjectMapper;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.service.IComboSubjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ComboSubjectService implements IComboSubjectService {

    SemesterRepo semesterRepo;
    SubjectRepo subjectRepo;
    ComboSubjectRepo comboSubjectRepo;
    SubjectMapper subjectMapper;
    ComboSubjectMapper comboSubjectMapper;

    private ComboSubjectResponse mapToComboSubjectResponse(ComboSubject combo) {
        ComboSubjectResponse response = comboSubjectMapper.toComboSubjectResponse(combo);
        List<SubjectResponse> subjects = subjectRepo.findByComboSubjectComboId(combo.getComboId())
                .stream().map(subjectMapper::toSubjectResponse).collect(Collectors.toList());
        response.setSubjects(subjects);
        return response;
    }

    @Override
    public List<ComboSubjectResponse> getAllComboSubjects() {
        return comboSubjectRepo.findByIsDeletedFalse().stream()
                .map(this::mapToComboSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ComboSubjectResponse getComboSubjectById(Long comboId) {
        ComboSubject combo = comboSubjectRepo.findById(comboId)
                .orElseThrow(() -> new GlobalException(404, "ComboSubject not found"));
        return mapToComboSubjectResponse(combo);
    }

    @Override
    @Transactional
    public ComboSubjectResponse createComboSubject(ComboSubjectRequest request) {
        if (request.getSubjects() == null || request.getSubjects().isEmpty()) {
            throw new GlobalException(400, "Cannot create a Combo without Subjects");
        }

        ComboSubject combo = ComboSubject.builder()
                .comboCode(request.getComboCode())
                .comboName(request.getComboName())
                .build();
        combo = comboSubjectRepo.save(combo);

        for (SubjectRequest subReq : request.getSubjects()) {
            Semester semester = semesterRepo.findById(subReq.getSemesterId())
                    .orElseThrow(() -> new GlobalException(404, "Semester not found: " + subReq.getSemesterId()));
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
                .orElseThrow(() -> new GlobalException(404, "ComboSubject not found"));

        combo.setComboCode(request.getComboCode());
        combo.setComboName(request.getComboName());
        combo = comboSubjectRepo.save(combo);

        if (request.getSubjects() != null) {
            List<Subject> existingSubjects = subjectRepo.findByComboSubjectComboId(combo.getComboId());
            for (Subject s : existingSubjects) {
                s.setIsDeleted(true);
            }
            subjectRepo.saveAll(existingSubjects);

            for (SubjectRequest subReq : request.getSubjects()) {
                Semester semester = semesterRepo.findById(subReq.getSemesterId())
                        .orElseThrow(() -> new GlobalException(404, "Semester not found: " + subReq.getSemesterId()));
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
                .orElseThrow(() -> new GlobalException(404, "ComboSubject not found"));

        List<Subject> subjects = subjectRepo.findByComboSubjectComboId(combo.getComboId());
        for (Subject subject : subjects) {
            subject.setIsDeleted(true);
        }
        if (!subjects.isEmpty()) {
            subjectRepo.saveAll(subjects);
        }

        combo.setIsDeleted(true);
        comboSubjectRepo.save(combo);
        return comboSubjectMapper.toDeleteResponse(combo, LocalDateTime.now());
    }

    @Override
    public List<ComboSubjectResponse> searchComboSubjects(String keyword) {
        return comboSubjectRepo.findByComboNameContainingIgnoreCase(keyword).stream()
                .filter(combo -> !combo.getIsDeleted())
                .map(this::mapToComboSubjectResponse)
                .collect(Collectors.toList());
    }
}
