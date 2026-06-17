package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.SemesterComboSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterComboSubjectRepo extends JpaRepository<SemesterComboSubject, Long> {
}
