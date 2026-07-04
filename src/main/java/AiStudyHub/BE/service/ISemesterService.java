package AiStudyHub.BE.service;

import AiStudyHub.BE.dto.Request.SemesterRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.SemesterResponse;

import java.util.List;

public interface ISemesterService {
    List<SemesterResponse> getAllSemesters();
    SemesterResponse getSemesterById(Long semesterId);
    SemesterResponse createSemester(SemesterRequest request);
    SemesterResponse updateSemester(Long semesterId, SemesterRequest request);
    DeleteResponse deleteSemester(Long semesterId);
}
