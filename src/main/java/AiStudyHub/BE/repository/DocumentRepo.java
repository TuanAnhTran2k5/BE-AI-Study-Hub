package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {


    List<Document> findByOwner_UserIdOrVisibilityStatus(Long userId, VisibilityStatus visibilityStatus);


    long deleteByDocumentId(Long documentId);

    List<Document> findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus visibilityStatus,
            Integer minCount);

    List<Document> findByVisibilityStatus(VisibilityStatus visibilityStatus);

    List<Document> findByVisibilityStatusAndSimHashContentIsNotNull(VisibilityStatus visibilityStatus);

    List<Document> findByOwner(AiStudyHub.BE.entity.User owner);

    List<Document> findByTitleContainingIgnoreCase(String keyword);
}
