package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepo extends JpaRepository<Badge, Long> {
    Optional<Badge> findByBadgeName(String badgeName);
    List<Badge> findByBadgeNameContainingIgnoreCase(String keyword);
}
