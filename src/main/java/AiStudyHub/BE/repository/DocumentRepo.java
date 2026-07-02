package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {


    List<Document> findByOwnerUserIdOrVisibilityStatus(Long userId, VisibilityStatus visibilityStatus);


    long deleteByDocumentId(Long documentId);

    List<Document> findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus visibilityStatus,
            Integer minCount);

    List<Document> findByVisibilityStatus(VisibilityStatus visibilityStatus);

    List<Document> findByVisibilityStatusAndSimHashContentIsNotNull(VisibilityStatus visibilityStatus);

    List<Document> findByOwner(AiStudyHub.BE.entity.User owner);
    List<Document> findByOwnerUserId(Long userId);

    List<Document> findByTitleContainingIgnoreCase(String keyword);

    long countByOwnerUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.downloadCount), 0) FROM Document d WHERE d.owner.userId = :userId")
    long sumDownloadCountByOwnerUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
