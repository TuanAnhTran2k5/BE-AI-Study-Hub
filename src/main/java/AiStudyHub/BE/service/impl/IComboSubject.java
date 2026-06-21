package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.ComboSubjectRequest;
import AiStudyHub.BE.dto.Response.ComboSubjectResponse;
import AiStudyHub.BE.dto.Response.DeleteResponse;

import java.util.List;

public interface IComboSubject {
    List<ComboSubjectResponse> getAllComboSubjects();
    ComboSubjectResponse getComboSubjectById(Long comboId);
    ComboSubjectResponse createComboSubject(ComboSubjectRequest request);
    ComboSubjectResponse updateComboSubject(Long comboId, ComboSubjectRequest request);
    DeleteResponse deleteComboSubject(Long comboId);
    List<ComboSubjectResponse> searchComboSubjects(String keyword);
}
