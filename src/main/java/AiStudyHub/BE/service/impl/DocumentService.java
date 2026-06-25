package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.DownloadRepo;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IDocument;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.IRagSystem;
import AiStudyHub.BE.service.IStorageService;
import AiStudyHub.BE.service.ISupabaseStorage;
import AiStudyHub.BE.service.IDuplicateCheck;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentService implements IDocument {

    DocumentRepo documentRepo;
    UserRepo userRepo;
    SubjectRepo subjectRepo;
    DownloadRepo downloadRepo;
    DocumentMapper documentMapper;
    IStorageService storageService;
    ISupabaseStorage supabaseStorageService;
    IDuplicateCheck duplicateCheckService;
    IGamification gamificationService;
    DocumentRagIndexer documentRagIndexer;
    RagDocumentRepository ragDocumentRepository;
    IRagSystem ragSystemService;

    @Autowired
    @Lazy
    @NonFinal
    IDocument self;

    // --- UPLOAD & UPDATE & DELETE ---

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

        VisibilityStatus visibilityStatus =
                request.getVisibilityStatus() == null
                        ? VisibilityStatus.PRIVATE
                        : request.getVisibilityStatus();

        Document document = documentMapper.toDocument(request);
        document.setOwner(owner);
        document.setSubject(subject);
        document.setFileName(request.getFile().getOriginalFilename());
        document.setFileType(request.getFile().getContentType());
        document.setFileSize(fileSize);
        document.setVisibilityStatus(visibilityStatus);
        document.setModerationStatus(ModerationStatus.NORMAL);
        document.setUploadStatus(UploadStatus.UPLOADING);
        document.setAverageRating(0.0);
        document.setRatingCount(0);
        document.setDownloadCount(0);
        document.setBookmarkCount(0);
        document.setReportCount(0);

        document = documentRepo.save(document);

        // Trigger Async Upload Process
        self.uploadDocumentAsync(
                document.getDocumentId(),
                fileBytes,
                request.getFile().getOriginalFilename(),
                request.getFile().getContentType(),
                fileSize,
                owner.getUserId()
        );

        DocumentUploadResponse response = documentMapper.toDocumentUploadResponse(document);
        response.setMessage("Your file is uploading. The process will complete in the background.");
        return response;
    }

    @Override
    @Async
    public void uploadDocumentAsync(Long documentId, byte[] fileBytes, String originalFileName, String contentType, Long fileSize, Long ownerId) {
        log.info("Starting async upload process for documentId: {}", documentId);
        
        FileUploadResponse fileMetadata = null;
        try {
            // 1. Upload to Supabase
            fileMetadata = supabaseStorageService.uploadBytes(fileBytes, originalFileName, null, contentType);
            log.info("Async uploaded file to Supabase successfully: {}", fileMetadata.getPublicUrl());

            // 2. Fetch document and owner
            Document document = documentRepo.findById(documentId).orElse(null);
            if (document == null) {
                log.error("Document not found for ID: {}. Deleting uploaded file.", documentId);
                safeDeleteFile(fileMetadata.getPublicUrl());
                return;
            }

            User owner = userRepo.findById(ownerId).orElse(null);
            if (owner == null) {
                log.error("Owner not found for ID: {}. Deleting uploaded file.", ownerId);
                safeDeleteFile(fileMetadata.getPublicUrl());
                return;
            }

            // 3. Update document metadata
            document.setFileName(fileMetadata.getOriginalFileName());
            document.setFileUrl(fileMetadata.getPublicUrl());
            document.setFileType(fileMetadata.getContentType());
            document.setFileSize(fileMetadata.getFileSize());

            // 4. Duplicate Check
            Document duplicatedDoc = duplicateCheckService.performDuplicateCheck(document.getDocumentId(), fileBytes);
            if (duplicatedDoc != null) {
                document.setVisibilityStatus(VisibilityStatus.PRIVATE);
                log.info("Duplication detected! Document set to PRIVATE. Duplicated with doc ID: {}", duplicatedDoc.getDocumentId());
            }

            // 5. Update user storage usage
            storageService.increaseStorage(owner, fileSize);
            userRepo.save(owner);

            // 6. Award badges / check achievements
            gamificationService.checkAndAwardBadges(owner.getUserId());

            // 7. Auto index RAG
            documentRagIndexer.autoIndexIfSupported(document, fileBytes);

            // 8. Update status to COMPLETED
            document.setUploadStatus(UploadStatus.COMPLETED);
            documentRepo.save(document);

            log.info("Async upload process finished successfully for documentId: {}", documentId);

        } catch (Exception e) {
            log.error("Async upload process failed for documentId: {}", documentId, e);
            if (fileMetadata != null) {
                safeDeleteFile(fileMetadata.getPublicUrl());
            }
            
            Document document = documentRepo.findById(documentId).orElse(null);
            if (document != null) {
                document.setUploadStatus(UploadStatus.FAILED);
                documentRepo.save(document);
            }
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
            ragSystemService.deleteDocument(documentId);
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
        User currentUser = SecurityUtils.getCurrentUser(); // 401 if not authenticated

        Document document = documentRepo.findById(documentId) // 404 if not found
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getUserId().equals(currentUser.getUserId())) { // 403 if not owner
            throw new GlobalException(ErrorCode.FORBIDDEN_UPDATE_DOCUMENT);
        }

        applyPartialUpdate(document, request);

        Document saved = documentRepo.save(document); // @PreUpdate

        return documentMapper.toDocumentUpdateResponse(saved);
    }

    @Override
    public List<DocumentResponse> searchDocumentsByTitle(String keyword) {
        return documentRepo.findByTitleContainingIgnoreCase(keyword).stream()
                .filter(doc -> doc.getVisibilityStatus() == VisibilityStatus.PUBLIC && doc.getUploadStatus() == UploadStatus.COMPLETED)
                .map(documentMapper::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getMyDocuments(Long userId) {
        return documentRepo.findByOwnerUserId(userId).stream()
                .map(documentMapper::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocumentDetail(Long documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getVisibilityStatus() == VisibilityStatus.PRIVATE) {
            User currentUser = SecurityUtils.getCurrentUser();
            if (!document.getOwner().getUserId().equals(currentUser.getUserId())) {
                throw new GlobalException(403, "You do not have permission to view this document");
            }
        } else {
            // For public documents, make sure it is COMPLETED
            if (document.getUploadStatus() != UploadStatus.COMPLETED) {
                throw new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND);
            }
        }

        return documentMapper.toDocumentResponse(document);
    }

    // --- DOWNLOAD ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception {
        Document publicDocument = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (publicDocument.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
        }

        if (publicDocument.getUploadStatus() != UploadStatus.COMPLETED) {
            throw new GlobalException(400, "This document is not fully uploaded yet.");
        }

        User authUser = SecurityUtils.getCurrentUser();

        User currentUser = userRepo.findById(authUser.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        User publicOwner = publicDocument.getOwner();
        String publicOwnerName = publicOwner.getFullName();

        byte[] fileBytes = supabaseStorageService.downloadFile(publicDocument.getFileUrl());

        storageService.validateStorage(currentUser, fileBytes.length);

        String personalFolder = "users/" + currentUser.getUserId() + "/documents";

        FileUploadResponse uploadResponse = supabaseStorageService.uploadBytes(
                fileBytes,
                publicDocument.getFileName(),
                personalFolder,
                publicDocument.getFileType()
        );

        // The copy already exists in storage. If any DB step below fails, the
        // transaction rolls back but the copied file would be orphaned, so we
        // remove it in the compensating catch block.
        try {
            Document privateDocument = Document.builder()
                    .owner(currentUser)
                    .subject(publicDocument.getSubject())
                    .title(publicDocument.getTitle())
                    .visibilityStatus(VisibilityStatus.PRIVATE)
                    .moderationStatus(publicDocument.getModerationStatus())
                    .averageRating(0.0)
                    .fileName(uploadResponse.getStoredFileName())
                    .fileUrl(uploadResponse.getPublicUrl())
                    .fileType(uploadResponse.getContentType())
                    .fileSize(uploadResponse.getFileSize())
                    .simHashContent(publicDocument.getSimHashContent())
                    .build();

            privateDocument = documentRepo.save(privateDocument);
            storageService.increaseStorage(currentUser, uploadResponse.getFileSize());
            userRepo.save(currentUser);

            boolean firstDownload = !downloadRepo.existsByUserAndDocument(currentUser, publicDocument);
            Integer addedPoint = 0;

            publicDocument.setDownloadCount(
                    publicDocument.getDownloadCount() == null ? 1 : publicDocument.getDownloadCount() + 1
            );
            documentRepo.save(publicDocument);

            Download download = Download.builder()
                    .user(currentUser)
                    .document(publicDocument)
                    .scoreAwarded(false)
                    .build();

            if (firstDownload) {
                if (!publicOwner.getUserId().equals(currentUser.getUserId())) {
                    addedPoint = gamificationService.awardScore(
                            publicOwner,
                            publicDocument,
                            new IGamification.ScoreTypeSpec("DOC_DOWNLOAD", "Document Download", 5,
                                    "Score awarded when another user downloads your document"),
                            5,
                            "Awarded 5 points because " + currentUser.getFullName()
                                    + " downloaded your document: " + publicDocument.getTitle());

                    download.setScoreAwarded(true);

                    // Re-evaluate rank due to score change
                    gamificationService.updateUserRank(publicOwner.getUserId());
                    gamificationService.addWeeklyScore(publicOwner.getUserId(), addedPoint);
                }
            }

            downloadRepo.save(download);

            // Re-evaluate badges due to download count increase
            gamificationService.checkAndAwardBadges(publicOwner.getUserId());

            DocumentDownloadResponse response = documentMapper.toDocumentDownloadResponse(
                    privateDocument,
                    firstDownload,
                    addedPoint,
                    publicOwner.getTotalScore(),
                    LocalDateTime.now()
            );

            response.setPublicOwnerName(publicOwnerName);

            return response;

        } catch (Exception ex) {
            // Compensating action: remove the orphaned copied file from storage.
            safeDeleteFile(uploadResponse.getPublicUrl());
            throw ex;
        }
    }

    @Override
    public ResponseEntity<Resource> downloadMyCloudDocument(Long documentId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean isOwner = document.getOwner()
                .getUserId()
                .equals(currentUser.getUserId());

        if (!isOwner) {
            throw new GlobalException(ErrorCode.FORBIDDEN_DOWNLOAD_CLOUD_DOCUMENT);
        }

        byte[] fileBytes = supabaseStorageService.downloadFile(document.getFileUrl());

        Resource resource = new ByteArrayResource(fileBytes);

        String fileName = document.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "document";
        }

        MediaType mediaType = getMediaType(document.getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @Override
    public ResponseEntity<Resource> viewDocumentContent(Long documentId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getVisibilityStatus() == VisibilityStatus.PRIVATE) {
            if (!document.getOwner().getUserId().equals(currentUser.getUserId())) {
                throw new GlobalException(403, "You do not have permission to view the content of this document");
            }
        }

        byte[] fileBytes = supabaseStorageService.downloadFile(document.getFileUrl());
        Resource resource = new ByteArrayResource(fileBytes);

        String fileName = document.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "document";
        }

        MediaType mediaType = getMediaType(document.getFileType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    // --- UTILS ---

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

    private MediaType getMediaType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return switch (fileType.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "doc", "docx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "ppt", "pptx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            case "xls", "xlsx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
