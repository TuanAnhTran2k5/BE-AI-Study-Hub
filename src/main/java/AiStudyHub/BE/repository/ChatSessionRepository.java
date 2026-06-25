package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    Optional<ChatSession> findBySessionIdAndUser_UserId(Long sessionId, Long userId);
}
