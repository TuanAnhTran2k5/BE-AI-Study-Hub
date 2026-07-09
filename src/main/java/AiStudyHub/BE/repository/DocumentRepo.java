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

    long sumDownloadCountByOwnerUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    List<Document> findBySourceDocumentDocumentId(Long sourceDocumentId);

    long countByUploadStatusAndModerationStatusAndDeletedAtIsNull(
            AiStudyHub.BE.constraint.UploadStatus u, AiStudyHub.BE.constraint.ModerationStatus m);

    long countByUploadStatusAndModerationStatusAndDeletedAtIsNullAndCreatedAtBetween(
            AiStudyHub.BE.constraint.UploadStatus u, AiStudyHub.BE.constraint.ModerationStatus m,
            java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countByUploadStatusAndDeletedAtIsNull(AiStudyHub.BE.constraint.UploadStatus status);

    long countByReportCountGreaterThanAndModerationStatusAndDeletedAtIsNull(
            int min, AiStudyHub.BE.constraint.ModerationStatus status);

    java.util.List<Object[]> countDocumentsGroupBySubject();

    org.springframework.data.domain.Page<Document> findPopularDocuments(org.springframework.data.domain.Pageable pageable);

    java.util.List<Object[]> countNewDocumentsByDate(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    java.util.List<Object[]> countActiveDocumentsGroupByOwnerIds(@org.springframework.data.repository.query.Param("ids") java.util.List<Long> ids);

    long sumFileSizeOfActiveDocuments();

    java.util.List<Object[]> countActiveDocumentsGroupByFileType();
}
