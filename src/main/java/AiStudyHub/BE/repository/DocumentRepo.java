package AiStudyHub.BE.repository;

import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {


    List<Document> findByOwnerUserIdOrVisibilityStatus(
            @Param("userId") Long userId,
            @Param("visibilityStatus") VisibilityStatus visibilityStatus
    );


    long deleteByDocumentId(Long documentId);

    List<Document> findByVisibilityStatusAndRatingCountGreaterThanEqual(
            @Param("visibilityStatus") VisibilityStatus visibilityStatus,
            @Param("minCount") Integer minCount
    );

    List<Document> findByVisibilityStatus(
            @Param("visibilityStatus") VisibilityStatus visibilityStatus
    );


    List<Document> findByOwner(User owner);
    List<Document> findByOwnerUserId(Long userId);

    List<Document> findByTitleContainingIgnoreCase(String keyword);

    long countByOwnerUserId(Long userId);

    long sumDownloadCountByOwnerUserId(@Param("userId") Long userId);

    List<Document> findBySourceDocumentDocumentId(Long sourceDocumentId);

    long countByUploadStatusAndModerationStatusAndDeletedAtIsNull(
            UploadStatus u, ModerationStatus m);

    long countByUploadStatusAndModerationStatusAndDeletedAtIsNullAndCreatedAtBetween(
            UploadStatus u, ModerationStatus m,
            LocalDateTime start, LocalDateTime end);

    long countByUploadStatusAndDeletedAtIsNull(UploadStatus status);

    long countByReportCountGreaterThanAndModerationStatusAndDeletedAtIsNull(
            int min, ModerationStatus status);

    List<Object[]> countDocumentsGroupBySubject();

    Page<Document> findPopularDocuments(Pageable pageable);

    List<Object[]> countNewDocumentsByDate(@Param("since") LocalDateTime since);

    List<Object[]> countActiveDocumentsGroupByOwnerIds(@Param("ids") List<Long> ids);

    long sumFileSizeOfActiveDocuments();

    List<Object[]> countActiveDocumentsGroupByFileType();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Document> findByDocumentId(Long documentId);

}
