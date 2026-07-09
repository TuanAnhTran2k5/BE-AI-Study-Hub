package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.CaseStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.ReportCase;
import AiStudyHub.BE.entity.ReportReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCaseRepo extends JpaRepository<ReportCase, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReportCase> findFirstByDocumentAndReasonAndCaseStatusIn(
            Document document,
            ReportReason reason,
            List<CaseStatus> statuses
    );

    List<ReportCase> findAllByCaseStatus(CaseStatus status);

    List<ReportCase> findAllByCaseStatusInOrderByResolvedAtDesc(List<CaseStatus> statuses);

    long deleteByDocumentDocumentId(Long documentId);

    boolean existsByReason(ReportReason reason);

    long countByCaseStatus(CaseStatus status);
}
