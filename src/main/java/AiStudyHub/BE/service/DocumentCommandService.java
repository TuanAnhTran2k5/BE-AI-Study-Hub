package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.impl.IDocumentCommand;
import AiStudyHub.BE.service.impl.IDuplicateCheck;
import AiStudyHub.BE.service.impl.IRagDocument;
import AiStudyHub.BE.service.impl.IRankingBadgeService;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentCommandService implements IDocumentCommand {

    DocumentRepo documentRepo;
    UserRepo userRepo;
    SubjectRepo subjectRepo;
    DocumentMapper documentMapper;
    IStorageService storageService;
    ISupabaseStorage supabaseStorageService;
    IDuplicateCheck duplicateCheckService;
    IRankingBadgeService rankingBadgeService;
    DocumentRagIndexer documentRagIndexer;
    RagDocumentRepository ragDocumentRepository;
    IRagDocument ragDocumentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request) throws Exception {
        User owner = userRepo.findById(request.getOwnerId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Subject subject = subjectRepo.findById(request.getSubjectId())
                .orElseThrow(() -> new GlobalException(ErrorCode.SUBJECT_NOT_FOUND));

        long fileSize = request.getFile().getSize();

        // Check file <= 20MB and total user capacity <= 2GB
        storageService.validateStorage(owner, fileSize);

        byte[] fileBytes = request.getFile().getBytes();

        FileUploadResponse fileMetadata = supabaseStorageService.uploadFile(request.getFile(), null);

        // File is already on Supabase. If any later DB step fails, the file would
        // become an orphan, so clean it up before propagating the error.
        try {
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
            document.setVisibilityStatus(visibilityStatus);
            document.setModerationStatus(ModerationStatus.NORMAL);
            document.setAverageRating(0.0);
            document.setRatingCount(0);
            document.setDownloadCount(0);
            document.setBookmarkCount(0);
            document.setReportCount(0);

            document = documentRepo.save(document);

            // Trigger Synchronous Duplicate Check
            Document duplicatedDoc = duplicateCheckService.performDuplicateCheck(document.getDocumentId(), fileBytes);

            // After uploading + saving the document, add the capacity
            storageService.increaseStorage(owner, fileSize);
            userRepo.save(owner);

            rankingBadgeService.checkAndAwardBadges(owner.getUserId());

            // Auto-index in RAG system if it's a supported format (best-effort)
            documentRagIndexer.autoIndexIfSupported(document, fileBytes);

            DocumentUploadResponse response = documentMapper.toDocumentUploadResponse(document);
            if (duplicatedDoc != null) {
                response.setVisibilityStatus(VisibilityStatus.PRIVATE);
                response.setMessage(String.format(
                        "Your file is uploaded successfully, but it is set to private mode due to duplication with document '%s' (File: %s).",
                        duplicatedDoc.getTitle(), duplicatedDoc.getFileName()));
            }
            return response;

        } catch (Exception ex) {
            // Compensating action: remove the orphaned file from storage.
            safeDeleteFile(fileMetadata.getPublicUrl());
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResponse deleteDocument(Long documentId) throws Exception {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Authorize: Only the owner or an administrator can delete the document
        User currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("AD");
        boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new GlobalException(403, "You do not have permission to delete this document");
        }

        DeleteResponse response = documentMapper.toDeleteResponse(document, LocalDateTime.now());

        Long fileSize = document.getFileSize() == null ? 0L : document.getFileSize();

        User owner = document.getOwner();
        String fileUrl = document.getFileUrl();

        // 1. Delete associated RAG resources (Qdrant vectors, chunks, rag_document metadata)
        //    in the SAME transaction. If RAG cleanup fails, the whole delete rolls back, so we
        //    never leave document/RAG data inconsistent (and never hit a FK violation).
        RagDocument ragDoc = ragDocumentRepository.findByDocument_DocumentId(documentId).orElse(null);
        if (ragDoc != null) {
            log.info("Deleting RAG resources for document ID: {}", documentId);
            ragDocumentService.deleteDocument(documentId);
        }

        // Do all DB work first so the transaction can roll back cleanly if anything fails.
        long deletedRows = documentRepo.deleteByDocumentId(documentId);

        if (deletedRows == 0) {
            throw new GlobalException(ErrorCode.DOCUMENT_DELETE_FAILED);
        }

        storageService.decreaseStorage(owner, fileSize);
        userRepo.save(owner);

        // External storage deletion happens LAST: if it throws, the DB transaction
        // rolls back and the document row + file both remain (consistent state).
        supabaseStorageService.deleteFile(fileUrl);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();                       // 401 if not authenticated

        Document document = documentRepo.findById(documentId)                    // 404 if not found
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getUserId().equals(currentUser.getUserId())) {  // 403 if not owner
            throw new GlobalException(ErrorCode.FORBIDDEN_UPDATE_DOCUMENT);
        }

        applyPartialUpdate(document, request);

        Document saved = documentRepo.save(document);                            // @PreUpdate

        return documentMapper.toDocumentUpdateResponse(saved);
    }

    @Override
    public java.util.List<DocumentUploadResponse> searchDocumentsByTitle(String keyword) {
        return documentRepo.findByTitleContainingIgnoreCase(keyword).stream()
                .filter(doc -> doc.getVisibilityStatus() == VisibilityStatus.PUBLIC)
                .map(documentMapper::toDocumentUploadResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // --------------------------------------------------------------------------------------------

    private void applyPartialUpdate(Document document, DocumentUpdateRequest request) {
        // title
        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                throw new GlobalException(ErrorCode.FIELD_REQUIRED);
            }
            document.setTitle(request.getTitle().trim());
        } // else: keep current

        // subjectId
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepo.findById(request.getSubjectId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.SUBJECT_NOT_FOUND));
            document.setSubject(subject);
        } // else: keep current

        // visibilityStatus
        if (request.getVisibilityStatus() != null) {
            document.setVisibilityStatus(request.getVisibilityStatus());
        } // else: keep current
    }

    private void safeDeleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            supabaseStorageService.deleteFile(fileUrl);
        } catch (Exception cleanupError) {
            log.error("Failed to clean up orphaned file after rollback: url={}, reason={}",
                    fileUrl, cleanupError.getMessage());
        }
    }
}
