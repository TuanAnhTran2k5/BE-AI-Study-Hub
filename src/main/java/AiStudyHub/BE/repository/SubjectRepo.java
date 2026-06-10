package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubjectRepo extends JpaRepository<Subject, Long> {

    List<Subject> findBySemester_SemesterId(Long semesterId);

    @Query("""
        SELECT s FROM Subject s
        WHERE s.semester.semesterId = :semesterId
        AND (
            s.comboSubject IS NULL
            OR s.comboSubject.comboId = :comboId
        )
    """)
    List<Subject> findSubjectsBySemesterAndCombo(Long semesterId, Long comboId);
}