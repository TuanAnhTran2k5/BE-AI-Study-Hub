package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DownloadRepo extends JpaRepository<Download, Long> {

    boolean existsByUserAndDocument(User user, Document document);

    long deleteByDocumentDocumentId(Long documentId);

    boolean existsByUserUserIdAndDocumentSimHashContent(Long userId, String simHashContent);

    Optional<Download> findFirstByUserUserIdAndDocumentSimHashContent(Long userId, String simHashContent);

    long deleteByUserUserIdAndDocumentDocumentId(Long userId, Long documentId);
}
