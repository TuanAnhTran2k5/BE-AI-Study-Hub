package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.UserRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRankRepo extends JpaRepository<UserRank, Long> {
    List<UserRank> findByUser(User user);
}
