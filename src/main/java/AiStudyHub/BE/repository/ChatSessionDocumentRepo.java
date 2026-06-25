package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatSessionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionDocumentRepo extends JpaRepository<ChatSessionDocument, Long> {

    long deleteByDocumentDocumentId(Long documentId);
}
