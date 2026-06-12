package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Rating;
import AiStudyHub.BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepo extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserAndDocument(User user, Document document);

    List<Rating> findByDocument(Document document);
}
