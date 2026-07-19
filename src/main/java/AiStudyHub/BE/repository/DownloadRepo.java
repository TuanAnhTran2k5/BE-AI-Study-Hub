package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.query.Param;

public interface DownloadRepo extends JpaRepository<Download, Long> {

    boolean existsByUserAndDocument(User user, Document document);

    long deleteByDocumentDocumentId(Long documentId);

    boolean existsByUserUserIdAndDocumentSimHashContent(Long userId, String simHashContent);

    Optional<Download> findFirstByUserUserIdAndDocumentSimHashContent(Long userId, String simHashContent);

    long deleteByUserUserIdAndDocumentDocumentId(Long userId, Long documentId);

    long countByDownloadedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Object[]> countDownloadsByDate(@Param("since") LocalDateTime since);

    List<Object[]> countDownloadsReceivedGroupByOwnerIds(@Param("ids") List<Long> ids);
}
