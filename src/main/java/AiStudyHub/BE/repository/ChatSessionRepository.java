package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ChatSession> findBySessionIdAndUserUserId(Long sessionId, Long userId);
}
