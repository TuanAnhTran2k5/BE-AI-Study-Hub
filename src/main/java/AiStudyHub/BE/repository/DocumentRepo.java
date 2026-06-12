package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {

    long deleteByDocumentId(Long documentId);

    // Documents eligible for the daily reputation job: PUBLIC and either already flagged as
    // having reached the rating threshold (sticky) OR currently at/above the threshold.
    @Query("SELECT d FROM Document d WHERE d.visibilityStatus = :status " +
            "AND (d.ratingThresholdReached = true OR d.ratingCount >= :minCount)")
    List<Document> findEligibleForReputation(@Param("status") VisibilityStatus status,
                                             @Param("minCount") int minCount);
}
