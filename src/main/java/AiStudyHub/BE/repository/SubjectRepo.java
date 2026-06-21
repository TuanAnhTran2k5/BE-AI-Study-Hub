package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepo extends JpaRepository<Subject, Long> {

    List<Subject> findBySemesterSemesterId(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectIsNull(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectComboId(Long semesterId, Long comboId);

    List<Subject> findByIsDeletedFalse();

    List<Subject> findBySubjectNameContainingIgnoreCase(String subjectName);

    List<Subject> findByComboSubjectComboId(Long comboId);
}