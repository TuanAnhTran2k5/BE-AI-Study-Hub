package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubjectRepo extends JpaRepository<Subject, Long> {

    List<Subject> findBySemester_SemesterId(Long semesterId);

    List<Subject> findBySemester_SemesterIdAndComboSubjectIsNull(Long semesterId);

    List<Subject> findBySemester_SemesterIdAndComboSubject_ComboId(Long semesterId, Long comboId);
}