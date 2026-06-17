package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Badge;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepo extends JpaRepository<UserBadge, Long> {
    boolean existsByUserAndBadge(User user, Badge badge);
    List<UserBadge> findByUser(User user);
}
