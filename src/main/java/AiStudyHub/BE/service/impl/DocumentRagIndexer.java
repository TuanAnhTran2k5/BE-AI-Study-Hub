package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.service.IRagSystem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentRagIndexer {

    RagDocumentRepository ragDocumentRepository;
    IRagSystem ragDocumentService;

    private boolean isSupported(String contentType, String fileName) {
        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith("audio/"))) {
            return false;
        }
        return true;
    }

    public void autoIndexIfSupported(Document document, byte[] fileBytes) {
        if (!isSupported(document.getFileType(), document.getFileName())) {
            log.info("Document '{}' type not supported for RAG indexing, skipping.", document.getFileName());
            return;
        }
        try {
            log.info("Auto-indexing document '{}' (ID: {}) in RAG system...",
                    document.getFileName(), document.getDocumentId());

            RagDocument ragDoc = RagDocument.builder()
                    .document(document)          // set the full entity so getDocumentId() works
                    .originalFileName(document.getFileName())
                    .contentType(document.getFileType())
                    .fileSize(document.getFileSize())
                    .uploadedBy(document.getOwner().getEmail())
                    .status("PENDING")
                    .build();

            ragDoc = ragDocumentRepository.save(ragDoc);

            // Explicitly re-attach the document reference after save,
            // because JPA may have detached or not eagerly loaded it.
            ragDoc.setDocument(document);

            ragDocumentService.indexDocumentContent(ragDoc, fileBytes);

            ragDoc.setStatus("INDEXED");
            ragDocumentRepository.save(ragDoc);
            log.info("Successfully auto-indexed document ID: {}", document.getDocumentId());
        } catch (Exception e) {
            log.error("Auto-indexing failed for document ID: {}. Document upload remains valid.",
                    document.getDocumentId(), e);
        }
    }
}
