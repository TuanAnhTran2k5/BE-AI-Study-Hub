package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;

import java.util.List;

public interface ISubjectService {
    List<SubjectResponse> getAllSubjects();
    SubjectResponse getSubjectById(Long subjectId);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(Long subjectId, SubjectRequest request);
    DeleteResponse deleteSubject(Long subjectId);
    List<SubjectResponse> getSubjectsBySemester(Long semesterId);
    List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId);
    List<SubjectResponse> searchSubjects(String keyword);
    SubjectResponse restoreSubject(Long subjectId);
}
