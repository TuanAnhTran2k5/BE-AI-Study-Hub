package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepo extends JpaRepository<Bookmark, Long> {

    @Query("SELECT b FROM Bookmark b JOIN FETCH b.document WHERE b.user.userId = :userId")
    List<Bookmark> findByUser_UserIdWithDocument(@Param("userId") Long userId);

    Optional<Bookmark> findByUser_UserIdAndDocument_DocumentId(Long userId, Long documentId);

    boolean existsByUser_UserIdAndDocument_DocumentId(Long userId, Long documentId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.userId = :userId AND b.document.documentId = :documentId")
    void deleteByUser_UserIdAndDocument_DocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);
}
