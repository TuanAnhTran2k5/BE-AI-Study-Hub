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
        String lower = fileName != null ? fileName.toLowerCase() : "";
        boolean byContentType = contentType != null && (
                contentType.equalsIgnoreCase("application/pdf") ||
                contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equalsIgnoreCase("text/plain"));
        return byContentType
                || lower.endsWith(".pdf")
                || lower.endsWith(".docx")
                || lower.endsWith(".txt");
    }

    public void autoIndexIfSupported(Document document, byte[] fileBytes) {
        if (!isSupported(document.getFileType(), document.getFileName())) {
            return;
        }
        try {
            log.info("Auto-indexing document '{}' in RAG system...", document.getFileName());
            RagDocument ragDoc = RagDocument.builder()
                    .document(document)
                    .originalFileName(document.getFileName())
                    .contentType(document.getFileType())
                    .fileSize(document.getFileSize())
                    .uploadedBy(document.getOwner().getEmail())
                    .status("PENDING")
                    .build();
            ragDoc = ragDocumentRepository.save(ragDoc);
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
