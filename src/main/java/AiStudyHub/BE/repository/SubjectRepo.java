package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepo extends JpaRepository<Subject, Long> {

    List<Subject> findBySemesterSemesterId(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectIsNull(Long semesterId);

    List<Subject> findBySemesterSemesterIdAndComboSubjectComboId(Long semesterId, Long comboId);

    List<Subject> findByIsDeletedFalse();

    Optional<Subject> findBySubjectCode(String subjectCode);

    List<Subject> findByComboSubjectComboId(Long comboId);
}