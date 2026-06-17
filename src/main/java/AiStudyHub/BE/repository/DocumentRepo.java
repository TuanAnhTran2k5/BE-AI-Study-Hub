package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

<<<<<<< HEAD
=======
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
>>>>>>> acdf6998a5aa3079e23570c32029962e37b4cc40
import java.util.List;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {

<<<<<<< HEAD
    List<Document> findByOwner_UserIdOrVisibilityStatus(Long userId, VisibilityStatus visibilityStatus);

=======
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
>>>>>>> acdf6998a5aa3079e23570c32029962e37b4cc40
    long deleteByDocumentId(Long documentId);

    List<Document> findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus visibilityStatus, Integer minCount);

    List<Document> findByVisibilityStatus(VisibilityStatus visibilityStatus);

    List<Document> findByVisibilityStatusAndSimHashContentIsNotNull(VisibilityStatus visibilityStatus);

    List<Document> findByOwner(AiStudyHub.BE.entity.User owner);
}
