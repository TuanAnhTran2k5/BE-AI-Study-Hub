package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ReportCase;
import AiStudyHub.BE.entity.ScoreLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreLogRepo extends JpaRepository<ScoreLog, Long> {

    boolean existsByActorUserIdAndDocumentIdAndScoreTypeTypeCode(Long actorUserId, Long documentId, String typeCode);
    long countByScoreTypeTypeCode(String typeCode);
    List<ScoreLog> findByDocumentIdAndScoreTypeTypeCode(Long documentId, String typeCode);
    List<ScoreLog> findByDocumentId(Long documentId);
    List<ScoreLog> findAllByReportCase(ReportCase reportCase);
}
