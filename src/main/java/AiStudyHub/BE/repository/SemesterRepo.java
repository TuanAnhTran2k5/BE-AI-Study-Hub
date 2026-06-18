package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepo extends JpaRepository<Semester, Long> {
    Optional<Semester> findBySemesterNo(String semesterNo);
}