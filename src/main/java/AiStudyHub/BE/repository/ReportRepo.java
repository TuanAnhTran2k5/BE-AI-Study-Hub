package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.ReportSeverity;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Report;
import AiStudyHub.BE.entity.ReportCase;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepo extends JpaRepository<Report, Long> {

    boolean existsByReporterAndDocument(User reporter, Document document);

    long countByDocument(Document document);

    long countByReporterAndReasonSeverityLevelAndCreatedAtAfter(
            User reporter,
            ReportSeverity severityLevel,
            LocalDateTime createdAt
    );

    List<Report> findAllByReportCase(ReportCase reportCase);

    long deleteByDocumentDocumentId(Long documentId);

    List<Report> findAllByReporterOrderByCreatedAtDesc(User reporter);

    boolean existsByReason(ReportReason reason);
}
