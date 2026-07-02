package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatSessionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionDocumentRepository extends JpaRepository<ChatSessionDocument, Long> {
    List<ChatSessionDocument> findBySessionSessionId(Long sessionId);
    long deleteBySessionSessionId(Long sessionId);
}
