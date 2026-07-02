package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByTotalScoreGreaterThan(Long score);
    java.util.List<User> findAllByOrderByTotalScoreDescUserIdAsc();
    org.springframework.data.domain.Page<User> findAllByOrderByTotalScoreDescUserIdAsc(org.springframework.data.domain.Pageable pageable);
}
