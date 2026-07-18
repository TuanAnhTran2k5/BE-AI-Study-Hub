package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepo extends JpaRepository<Subject, Long> {

    List<Subject> findBySemesterSemesterId(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndIsDeletedFalse(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectIsNullAndIsDeletedFalse(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectComboIdAndIsDeletedFalse(Long semesterId, Long comboId);

    List<Subject> findByIsDeletedFalse();

    List<Subject> findBySubjectNameContainingIgnoreCaseAndIsDeletedFalse(String subjectName);

    List<Subject> findByComboSubjectComboIdAndIsDeletedFalse(Long comboId);

    List<Subject> findByComboSubjectComboId(Long comboId);

    Optional<Subject> findBySubjectCode(String subjectCode);

    boolean existsBySubjectCode(String subjectCode);
}