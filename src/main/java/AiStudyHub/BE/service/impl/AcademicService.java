package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.mapper.SubjectMapper;
import AiStudyHub.BE.repository.ComboSubjectRepo;
import AiStudyHub.BE.repository.SemesterRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.service.IAcademic;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicService implements IAcademic {

    SemesterRepo semesterRepo;
    ComboSubjectRepo comboSubjectRepo;
    SubjectRepo subjectRepo;
    SubjectMapper subjectMapper;

    @Override
    public List<Semester> getAllSemesters() {
        return semesterRepo.findAll();
    }

    @Override
    public List<ComboSubject> getAllCombos() {
        return comboSubjectRepo.findAll();
    }

    @Override
    public List<SubjectResponse> getSubjectsBySemester(Long semesterId) {
        return subjectRepo.findBySemesterSemesterId(semesterId)
                .stream()
                .map(subjectMapper::toSubjectResponse)
                .toList();
    }

    @Override
    public List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId) {
        List<Subject> baseSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectIsNull(semesterId);
        List<Subject> comboSubjects = subjectRepo.findBySemesterSemesterIdAndComboSubjectComboId(semesterId, comboId);

        return Stream.concat(baseSubjects.stream(), comboSubjects.stream())
                .map(subjectMapper::toSubjectResponse)
                .toList();
    }
}
