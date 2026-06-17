package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {

    @Query("""
        SELECT d.documentId FROM Document d
        WHERE d.owner.userId = :userId
        OR d.visibilityStatus = AiStudyHub.BE.constraint.VisibilityStatus.PUBLIC
    """)
    List<Long> findAccessibleDocumentIds(@Param("userId") Long userId);

    @Query("""
        SELECT d.documentId FROM Document d
        WHERE d.visibilityStatus = AiStudyHub.BE.constraint.VisibilityStatus.PUBLIC
    """)
    List<Long> findPublicDocumentIds();
    long deleteByDocumentId(Long documentId);

    List<Document> findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus visibilityStatus, Integer minCount);

    List<Document> findByVisibilityStatus(VisibilityStatus visibilityStatus);

    List<Document> findByVisibilityStatusAndSimHashContentIsNotNull(VisibilityStatus visibilityStatus);

    List<Document> findByOwner(AiStudyHub.BE.entity.User owner);
}
