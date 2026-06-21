package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Response.SubjectResponse;
import AiStudyHub.BE.entity.ComboSubject;
import AiStudyHub.BE.entity.Semester;

import java.util.List;

public interface IAcademic {
    List<Semester> getAllSemesters();
    List<ComboSubject> getAllCombos();
    List<SubjectResponse> getSubjectsBySemester(Long semesterId);
    List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId);
}
