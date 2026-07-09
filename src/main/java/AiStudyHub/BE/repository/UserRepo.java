package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByTotalScoreGreaterThan(Long score);
    java.util.List<User> findAllByOrderByTotalScoreDescUserIdAsc();
    org.springframework.data.domain.Page<User> findAllByOrderByTotalScoreDescUserIdAsc(org.springframework.data.domain.Pageable pageable);

    long countByStatus(AiStudyHub.BE.constraint.UserStatus status);
    long countByStatusAndCreatedAtBetween(AiStudyHub.BE.constraint.UserStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    org.springframework.data.domain.Page<User> findUsersForAdmin(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("status") AiStudyHub.BE.constraint.UserStatus status,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> findAllByStatusOrderByTotalScoreDescUserIdAsc(
            AiStudyHub.BE.constraint.UserStatus status, org.springframework.data.domain.Pageable pageable);

    java.util.List<Object[]> countSignupsByDate(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
