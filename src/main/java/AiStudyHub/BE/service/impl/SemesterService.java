package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.SemesterMapper;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.service.ISemesterService;
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
public class SemesterService implements ISemesterService {

    SemesterRepo semesterRepo;
    SemesterMapper semesterMapper;

    @Override
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepo.findAll().stream()
                .map(semesterMapper::toSemesterResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SemesterResponse getSemesterById(Long semesterId) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new GlobalException(404, "Semester not found"));
        return semesterMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        Semester semester = Semester.builder()
                .semesterNo(request.getSemesterNo())
                .description(request.getDescription())
                .build();
        semester = semesterRepo.save(semester);
        return semesterMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse updateSemester(Long semesterId, SemesterRequest request) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new GlobalException(404, "Semester not found"));
        semester.setSemesterNo(request.getSemesterNo());
        semester.setDescription(request.getDescription());
        semester = semesterRepo.save(semester);
        return semesterMapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public DeleteResponse deleteSemester(Long semesterId) {
        Semester semester = semesterRepo.findById(semesterId)
                .orElseThrow(() -> new GlobalException(404, "Semester not found"));
        semesterRepo.delete(semester);
        return semesterMapper.toDeleteResponse(semester, LocalDateTime.now());
    }
}
