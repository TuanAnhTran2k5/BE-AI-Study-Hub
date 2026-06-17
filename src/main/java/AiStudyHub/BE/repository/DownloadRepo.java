package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadRepo extends JpaRepository<Download, Long> {

    boolean existsByUserAndDocument(User user, Document document);
}
