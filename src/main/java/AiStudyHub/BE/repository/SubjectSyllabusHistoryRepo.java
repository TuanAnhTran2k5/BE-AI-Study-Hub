package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.SubjectSyllabusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectSyllabusHistoryRepo extends JpaRepository<SubjectSyllabusHistory, Long> {
    List<SubjectSyllabusHistory> findBySubjectSyllabusIdOrderByVersionDesc(Long subjectSyllabusId);
    Optional<SubjectSyllabusHistory> findFirstBySubjectSyllabusIdOrderByVersionDesc(Long subjectSyllabusId);
}
