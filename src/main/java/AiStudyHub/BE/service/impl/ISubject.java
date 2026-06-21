package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;

import java.util.List;

public interface ISubject {
    // Admin CRUD
    List<SubjectResponse> getAllSubjects();
    SubjectResponse getSubjectById(Long subjectId);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(Long subjectId, SubjectRequest request);
    DeleteResponse deleteSubject(Long subjectId);

    // Public reads (used by the academic endpoints)
    List<SubjectResponse> getSubjectsBySemester(Long semesterId);
    List<SubjectResponse> getSubjectsBySemesterAndCombo(Long semesterId, Long comboId);
    List<SubjectResponse> searchSubjects(String keyword);
}
