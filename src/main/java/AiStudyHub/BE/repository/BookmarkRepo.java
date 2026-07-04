package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepo extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserUserId(Long userId);

    Optional<Bookmark> findByUserUserIdAndDocumentDocumentId(Long userId, Long documentId);

    boolean existsByUserUserIdAndDocumentDocumentId(Long userId, Long documentId);

    long deleteByDocumentDocumentId(Long documentId);

    long deleteByUserUserIdAndDocumentDocumentId(Long userId, Long documentId);
    long countByUserUserId(Long userId);
    long countByDocumentDocumentId(Long documentId);
}
