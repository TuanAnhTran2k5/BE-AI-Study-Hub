package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.SubjectSyllabus;
import AiStudyHub.BE.entity.SubjectSyllabusHistory;
import java.util.List;

public interface ISyllabusService {
    SubjectSyllabus initSyllabusUpload(Long subjectId, String pdfUrl, byte[] pdfBytes, String adminUsername);
    void parseSyllabusAsync(Long syllabusId);
    void syncToVectorStore(Long syllabusId);
    SubjectSyllabus updateSyllabus(Long subjectId, String jsonContent, String adminUsername, String reason);
    List<SubjectSyllabusHistory> getHistory(Long subjectId);
    SubjectSyllabus rollback(Long subjectId, Long historyId, String adminUsername);
    void deleteSyllabus(Long subjectId);
}
