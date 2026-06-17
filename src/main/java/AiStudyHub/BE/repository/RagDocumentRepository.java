package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link RagDocument} entity.
 */
@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    /**
     * Finds a document by its original filename.
     *
     * @param originalFileName the original name of the file
     * @return an Optional holding the matching RagDocument
     */
    Optional<RagDocument> findByOriginalFileName(String originalFileName);

    Optional<RagDocument> findByDocumentId(Long documentId);
}
