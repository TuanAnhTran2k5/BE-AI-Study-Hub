package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.service.impl.IDocument;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class DocumentService implements IDocument {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepo documentRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private SubjectRepo subjectRepo;
    @Autowired
    private ISupabaseStorage supabaseStorageService;
    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private IStorageService storageService;

    @Autowired
    private RagDocumentRepository ragDocumentRepository;
    @Autowired
    private RagDocumentService ragDocumentService;

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception {

        User owner = userRepo.findById(request.getOwnerId())
                             .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Subject subject = subjectRepo.findById(request.getSubjectId())
                                     .orElseThrow(() -> new GlobalException(ErrorCode.SUBJECT_NOT_FOUND));

        long fileSize = request.getFile().getSize();

        // Check file <= 20MB and total user capacity <= 2GB
        storageService.validateUpload(owner, fileSize);

        byte[] fileBytes = request.getFile().getBytes();

        FileUploadResponse fileMetadata = supabaseStorageService.uploadFile(request.getFile(), null);


        VisibilityStatus visibilityStatus =
                request.getVisibilityStatus() == null
                        ? VisibilityStatus.PRIVATE
                        : request.getVisibilityStatus();

        Document document = documentMapper.toDocument(request);
        document.setOwner(owner);
        document.setSubject(subject);
        document.setFileName(fileMetadata.getOriginalFileName());
        document.setFileUrl(fileMetadata.getPublicUrl());
        document.setFileType(fileMetadata.getContentType());
        document.setFileSize(fileMetadata.getFileSize());
        document.setContentHashSha256(hashSha256(fileBytes));
        document.setVisibilityStatus(visibilityStatus);
        document.setModerationStatus(ModerationStatus.NORMAL);
        document.setAverageRating(0.0);
        document.setRatingCount(0);
        document.setDownloadCount(0);
        document.setBookmarkCount(0);
        document.setReportCount(0);

        document = documentRepo.save(document);

        // After uploading + saving the document, add the capacity
        storageService.increaseStorage(owner, fileSize);
        userRepo.save(owner);

        // Auto-index in RAG system if it's a supported format (PDF, DOCX, TXT)
        String contentType = fileMetadata.getContentType();
        if (contentType != null && (
                contentType.equals("application/pdf") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("text/plain") ||
                fileMetadata.getOriginalFileName().endsWith(".pdf") ||
                fileMetadata.getOriginalFileName().endsWith(".docx") ||
                fileMetadata.getOriginalFileName().endsWith(".txt")
        )) {
            try {
                logger.info("Auto-indexing document '{}' in RAG system...", document.getFileName());
                RagDocument ragDoc = RagDocument.builder()
                        .documentId(document.getDocumentId())
                        .originalFileName(document.getFileName())
                        .contentType(document.getFileType())
                        .fileSize(document.getFileSize())
                        .uploadedBy(owner.getEmail())
                        .status("PENDING")
                        .build();
                ragDoc = ragDocumentRepository.save(ragDoc);
                ragDocumentService.indexDocumentContent(ragDoc, fileBytes);
                ragDoc.setStatus("INDEXED");
                ragDocumentRepository.save(ragDoc);
                logger.info("Successfully auto-indexed document ID: {}", document.getDocumentId());
            } catch (Exception e) {
                logger.error("Auto-indexing failed for document ID: {}. Document upload remains valid.", document.getDocumentId(), e);
            }
        }

        return documentMapper.toDocumentUploadResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId, User currentUser) throws Exception {
        logger.info("User {} requesting deletion of document ID: {}", currentUser.getEmail(), documentId);

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(404, "Document not found"));

        // Authorize: Only the owner or an administrator can delete the document
        boolean isAdmin = currentUser.getRole().name().equals("AD");
        boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new GlobalException(403, "You do not have permission to delete this document");
        }

        // 1. Delete associated RAG resources (vectors from Qdrant, chunks from MySQL, RAG metadata)
        RagDocument ragDoc = ragDocumentRepository.findByDocumentId(documentId)
                .orElse(null);
        if (ragDoc != null) {
            try {
                logger.info("Deleting RAG resources for document ID: {}", documentId);
                ragDocumentService.deleteDocument(ragDoc.getId());
            } catch (Exception e) {
                logger.error("Error cleaning up RAG resources for document ID: {}", documentId, e);
            }
        }

        // 2. Delete physical file from Supabase storage
        String storagePath = extractStoragePath(document.getFileUrl());
        if (storagePath != null && !storagePath.isEmpty()) {
            try {
                logger.info("Deleting physical file from Supabase storage path: {}", storagePath);
                supabaseStorageService.deleteFile(storagePath);
            } catch (Exception e) {
                logger.error("Error deleting physical file from Supabase for document ID: {}", documentId, e);
            }
        }

        // 3. Reclaim user storage space quota
        long fileSize = document.getFileSize();
        User owner = document.getOwner();
        try {
            logger.info("Reclaiming {} bytes for user {}", fileSize, owner.getEmail());
            storageService.decreaseStorage(owner, fileSize);
            userRepo.save(owner);
        } catch (Exception e) {
            logger.error("Error reclaiming storage space quota for user {}", owner.getEmail(), e);
        }

        // 4. Delete the main document record from DB
        documentRepo.delete(document);
        logger.info("Successfully deleted document record ID: {}", documentId);
    }

    private String extractStoragePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        String bucketPart = "/public/Documents/";
        int index = fileUrl.indexOf(bucketPart);
        if (index != -1) {
            return fileUrl.substring(index + bucketPart.length());
        }
        
        int publicIndex = fileUrl.indexOf("/public/");
        if (publicIndex != -1) {
            String afterPublic = fileUrl.substring(publicIndex + "/public/".length());
            int firstSlash = afterPublic.indexOf('/');
            if (firstSlash != -1) {
                return afterPublic.substring(firstSlash + 1);
            }
        }
        return null;
    }

    private String hashSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}