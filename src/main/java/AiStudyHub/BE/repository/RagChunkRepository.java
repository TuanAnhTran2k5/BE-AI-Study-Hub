package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.RagChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    List<RagChunk> findByDocumentId(Long documentId);

    @Transactional
    void deleteByDocumentId(Long documentId);
}
