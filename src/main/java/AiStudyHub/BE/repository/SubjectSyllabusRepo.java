package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.SubjectSyllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubjectSyllabusRepo extends JpaRepository<SubjectSyllabus, Long> {
    Optional<SubjectSyllabus> findBySubjectSubjectId(Long subjectId);
    Optional<SubjectSyllabus> findBySubjectSubjectCode(String subjectCode);
}
