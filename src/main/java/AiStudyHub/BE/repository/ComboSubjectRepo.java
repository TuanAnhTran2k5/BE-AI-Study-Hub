package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ComboSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboSubjectRepo extends JpaRepository<ComboSubject, Long> {
    List<ComboSubject> findByComboNameContainingIgnoreCase(String keyword);
}
