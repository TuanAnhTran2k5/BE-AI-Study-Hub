package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD

=======
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
>>>>>>> ce577f9 (Add academic APIs for semesters, combos, subjects and document upload)
public interface SubjectRepo extends JpaRepository<Subject, Long> {
    List<Subject> findBySemester_SemesterId(Long semesterId);

    @Query("""
        SELECT s FROM Subject s
        WHERE s.semester.semesterId = :semesterId
        AND (
            s.subjectType = AiStudyHub.BE.constraint.SubjectType.CORE
            OR s.comboSubject.comboId = :comboId
        )
    """)
    List<Subject> findSubjectsBySemesterAndCombo(Long semesterId, Long comboId);
}
