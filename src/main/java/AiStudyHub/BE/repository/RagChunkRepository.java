package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.RagChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for {@link RagChunk} entity.
 */
@Repository
public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    /**
     * Retrieves all chunks belonging to a specific document ID.
     *
     * @param documentId the ID of the document
     * @return list of RagChunks
     */
    List<RagChunk> findByDocumentId(Long documentId);

    /**
     * Deletes all chunks associated with a specific document ID.
     *
     * @param documentId the ID of the document
     */
    @Transactional
    void deleteByDocumentId(Long documentId);
}
