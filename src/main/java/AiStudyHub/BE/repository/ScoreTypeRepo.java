package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ScoreType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ScoreTypeRepo extends JpaRepository<ScoreType, Long> {
    Optional<ScoreType> findByTypeCode(String typeCode);
}
