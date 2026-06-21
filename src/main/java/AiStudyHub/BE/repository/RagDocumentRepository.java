package AiStudyHub.BE.repository;

import AiStudyHub.BE.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    // Looks up the RagDocument by the linked library Document's id (derived query
    // navigating the document FK relationship: RagDocument.document.documentId).
    Optional<RagDocument> findByDocument_DocumentId(Long documentId);
}
