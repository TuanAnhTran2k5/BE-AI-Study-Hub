package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ComboSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComboSubjectRepo extends JpaRepository<ComboSubject, Long> {
    List<ComboSubject> findByIsDeletedFalse();
    Optional<ComboSubject> findByComboCode(String comboCode);
}