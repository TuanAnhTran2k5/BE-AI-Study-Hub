package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Request.SubjectRequest;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;
import AiStudyHub.BE.dto.Response.SubjectResponse;

import java.util.List;

public interface ICurriculum {
    // Semester
    List<SemesterResponse> getAllSemesters();
    SemesterResponse getSemesterById(Long semesterId);
    SemesterResponse createSemester(SemesterRequest request);
    SemesterResponse updateSemester(Long semesterId, SemesterRequest request);
    DeleteResponse deleteSemester(Long semesterId);

    // Subject
    List<SubjectResponse> getAllSubjects();
    SubjectResponse getSubjectById(Long subjectId);
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse updateSubject(Long subjectId, SubjectRequest request);
    DeleteResponse deleteSubject(Long subjectId);

    // ComboSubject
    List<ComboSubjectResponse> getAllComboSubjects();
    ComboSubjectResponse getComboSubjectById(Long comboId);
    ComboSubjectResponse createComboSubject(ComboSubjectRequest request);
    ComboSubjectResponse updateComboSubject(Long comboId, ComboSubjectRequest request);
    DeleteResponse deleteComboSubject(Long comboId);
}
