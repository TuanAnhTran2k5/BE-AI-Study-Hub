package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import AiStudyHub.BE.constraint.SenderType;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySession_SessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
    List<ChatMessage> findTop10BySession_SessionIdOrderByCreatedAtDesc(Long sessionId);
    long countBySession_SessionId(Long sessionId);

    long countBySenderType(SenderType senderType);
    long countBySenderTypeAndCreatedAtBetween(
            SenderType senderType, LocalDateTime start, LocalDateTime end);

    List<Object[]> countAiQueriesByDate(@Param("since") LocalDateTime since);
}
