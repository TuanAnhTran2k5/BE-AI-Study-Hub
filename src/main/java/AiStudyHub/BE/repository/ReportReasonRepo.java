package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportReasonRepo extends JpaRepository<ReportReason, Long> {
}
