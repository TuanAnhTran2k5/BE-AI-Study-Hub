package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Notification;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.NotificationRepo;
import AiStudyHub.BE.utils.SimHashUtil;
import AiStudyHub.BE.utils.TextExtractionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
public class DuplicateCheckService implements AiStudyHub.BE.service.IDuplicateCheck {

    @Autowired
    private TextExtractionUtil textExtractionUtil;

    @Autowired
    private SimHashUtil simHashUtil;

    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private NotificationRepo notificationRepo;

    
    // Synchronously checks if the uploaded document is a duplicate of an existing public document.
    // Uses SimHash to determine >= 90% text similarity.
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document performDuplicateCheck(Long documentId, byte[] fileBytes) {
        log.info("Starting duplicate check for Document ID: {}", documentId);

        try {
            // 1. Fetch document
            Document document = documentRepo.findById(documentId).orElse(null);
            if (document == null) {
                log.warn("Document ID {} not found. Aborting duplicate check.", documentId);
                return null;
            }

            // 2. Extract text from file bytes
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            String extractedText = textExtractionUtil.extractText(inputStream);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.info("No extractable text for Document ID: {}", documentId);
                return null;
            }

            // 3. Calculate SimHash
            String simHash = simHashUtil.calculateSimHash(extractedText);
            if (simHash == null) {
                return null;
            }

            // 4. Compare with existing PUBLIC documents
            List<Document> publicDocuments = documentRepo.findByVisibilityStatusAndSimHashContentIsNotNull(VisibilityStatus.PUBLIC)
                    .stream()
                    .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                    .toList();
            
            Document duplicatedDoc = null;
            for (Document pubDoc : publicDocuments) {
                // Skip self
                if (pubDoc.getDocumentId().equals(document.getDocumentId())) {
                    continue;
                }

                int distance = simHashUtil.calculateHammingDistance(simHash, pubDoc.getSimHashContent());
                // Distance <= 3 indicates high similarity (approx >= 90%)
                if (distance <= 3) {
                    duplicatedDoc = pubDoc;
                    log.info("Document ID {} is a duplicate of Document ID {}. Hamming distance: {}", 
                            documentId, pubDoc.getDocumentId(), distance);
                    break;
                }
            }

            // 5. Update document status and notify
            if (duplicatedDoc != null) {
                document.setVisibilityStatus(VisibilityStatus.PRIVATE);
                document.setSimHashContent(simHash);
                documentRepo.save(document);

                // Create Notification
                Notification notification = Notification.builder()
                        .user(document.getOwner())
                        .document(document)
                        .title("Duplicate Document")
                        .message(String.format("Your document '%s' has been set to Private. The system detected that it is a duplicate of document '%s' (File: %s).", 
                                document.getTitle(), duplicatedDoc.getTitle(), duplicatedDoc.getFileName()))
                        .type("SYSTEM")
                        .notificationCase("DUPLICATE_DOCUMENT")
                        .isRead(false)
                        .build();
                notificationRepo.save(notification);

            } else {
                // Not a duplicate, just save the SimHash for future checks
                document.setSimHashContent(simHash);
                documentRepo.save(document);
            }

            return duplicatedDoc;

        } catch (Exception e) {
            log.error("Error during duplicate check for Document ID: {}", documentId, e);
            return null;
        }
    }
}
