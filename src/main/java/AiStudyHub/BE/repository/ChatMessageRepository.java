package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySession_SessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
    List<ChatMessage> findTop10BySession_SessionIdOrderByCreatedAtDesc(Long sessionId);
    long countBySession_SessionId(Long sessionId);

    long countBySenderType(AiStudyHub.BE.constraint.SenderType senderType);
    long countBySenderTypeAndCreatedAtBetween(
            AiStudyHub.BE.constraint.SenderType senderType, java.time.LocalDateTime start, java.time.LocalDateTime end);

    java.util.List<Object[]> countAiQueriesByDate(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
