package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    Optional<RagDocument> findByOriginalFileName(String originalFileName);

    Optional<RagDocument> findByDocumentId(Long documentId);
}
