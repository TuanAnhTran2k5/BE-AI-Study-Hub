package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.WeeklyScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyScoreRepo extends JpaRepository<WeeklyScore, Long> {
    Optional<WeeklyScore> findByUserAndWeekStart(User user, LocalDate weekStart);
    List<WeeklyScore> findByWeekStart(LocalDate weekStart);
}
