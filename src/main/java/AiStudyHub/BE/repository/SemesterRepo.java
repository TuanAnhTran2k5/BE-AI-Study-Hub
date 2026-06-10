package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepo extends JpaRepository<Semester, Long> {
}