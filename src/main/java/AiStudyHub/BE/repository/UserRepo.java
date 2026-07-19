package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import AiStudyHub.BE.constraint.UserStatus;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByTotalScoreGreaterThan(Long score);
    List<User> findAllByOrderByTotalScoreDescUserIdAsc();
    Page<User> findAllByOrderByTotalScoreDescUserIdAsc(Pageable pageable);

    long countByStatus(UserStatus status);
    long countByStatusAndCreatedAtBetween(UserStatus status, LocalDateTime start, LocalDateTime end);

    Page<User> findUsersForAdmin(
            @Param("search") String search,
            @Param("status") UserStatus status,
            Pageable pageable);

    Page<User> findAllByStatusOrderByTotalScoreDescUserIdAsc(
            UserStatus status, Pageable pageable);

    List<Object[]> countSignupsByDate(@Param("since") LocalDateTime since);
}
