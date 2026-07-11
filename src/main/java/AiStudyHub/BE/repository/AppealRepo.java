package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.AppealStatus;
import AiStudyHub.BE.entity.Appeal;
import AiStudyHub.BE.entity.ReportCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppealRepo extends JpaRepository<Appeal, Long> {
    List<Appeal> findAllByStatus(AppealStatus status);
    List<Appeal> findAllByStatusOrderByCreatedAtDesc(AppealStatus status);
    List<Appeal> findAllByUserUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByReportCaseCaseIdAndStatus(Long caseId, AppealStatus status);
}
