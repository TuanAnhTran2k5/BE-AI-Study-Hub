package AiStudyHub.BE.service;


import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentDeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUpdateResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RagDocumentRepository;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.repository.DownloadRepo;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.service.impl.IDocument;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import AiStudyHub.BE.service.impl.IRankingBadgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class DocumentService implements IDocument {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepo documentRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private SubjectRepo subjectRepo;
    @Autowired
    private DownloadRepo downloadRepo;
    @Autowired
    private ScoreLogRepo scoreLogRepo;
    @Autowired
    private ScoreTypeRepo scoreTypeRepo;
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
    @Autowired
    private IRankingBadgeService rankingBadgeService;
    @Autowired
    private DuplicateCheckService duplicateCheckService;

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

            DocumentUploadResponse response = documentMapper.toDocumentUploadResponse(document);
            if (duplicatedDoc != null) {
                response.setVisibilityStatus(VisibilityStatus.PRIVATE);
                response.setMessage(String.format("Your file is uploaded successfully, but it is set to private mode due to duplication with document '%s' (File: %s).", 
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
    public DocumentDeleteResponse deleteDocument(Long documentId) throws Exception {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Authorize: Only the owner or an administrator can delete the document
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("AD");
        boolean isOwner = document.getOwner().getUserId().equals(currentUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw new GlobalException(403, "You do not have permission to delete this document");
        }

        DocumentDeleteResponse response = documentMapper.toDocumentDeleteResponse(
                document,
                LocalDateTime.now()
        );

        Long fileSize = document.getFileSize() == null ? 0L : document.getFileSize();

        User owner = document.getOwner();
        String fileUrl = document.getFileUrl();

        // 1. Delete associated RAG resources (vectors from Qdrant, chunks from MySQL, RAG metadata)
        RagDocument ragDoc = ragDocumentRepository.findByDocumentId(documentId)
                .orElse(null);
        if (ragDoc != null) {
            try {
                logger.info("Deleting RAG resources for document ID: {}", documentId);
                ragDocumentService.deleteDocument(documentId);
            } catch (Exception e) {
                logger.error("Error cleaning up RAG resources for document ID: {}", documentId, e);
            }
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
    public DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception {
        Document publicDocument = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (publicDocument.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
        }

        User authUser = getCurrentUser();

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
                    ScoreType scoreType = scoreTypeRepo.findByTypeCode("DOC_DOWNLOAD")
                            .orElseGet(() -> {
                                ScoreType newType = ScoreType.builder()
                                        .typeCode("DOC_DOWNLOAD")
                                        .typeName("Document Download")
                                        .defaultPoint(5)
                                        .description("Score awarded when another user downloads your document")
                                        .build();
                                return scoreTypeRepo.save(newType);
                            });

                    Long currentScore = publicOwner.getTotalScore() == null ? 0L : publicOwner.getTotalScore();
                    addedPoint = scoreType.getDefaultPoint();
                    publicOwner.setTotalScore(currentScore + addedPoint);
                    userRepo.save(publicOwner);

                    ScoreLog scoreLog = ScoreLog.builder()
                            .user(publicOwner)
                            .document(publicDocument)
                            .scoreType(scoreType)
                            .scoreChange(addedPoint)
                            .description("Awarded " + addedPoint + " points because " + currentUser.getFullName() + " downloaded your document: " + publicDocument.getTitle())
                            .build();
                    scoreLogRepo.save(scoreLog);

                    download.setScoreAwarded(true);

                    // Re-evaluate rank due to score change
                    rankingBadgeService.updateUserRank(publicOwner.getUserId());
                    rankingBadgeService.addWeeklyScore(publicOwner.getUserId(), addedPoint);
                }
            }

            downloadRepo.save(download);

            // Re-evaluate badges due to download count increase
            rankingBadgeService.checkAndAwardBadges(publicOwner.getUserId());

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
        User currentUser = getCurrentUser();

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
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .body(resource);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUpdateResponse updateDocument(Long documentId, DocumentUpdateRequest request) {
        User currentUser = getCurrentUser();                       // 401 if not authenticated

        Document document = documentRepo.findById(documentId)      // 404 if not found
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getUserId().equals(currentUser.getUserId())) { // 403 if not owner
            throw new GlobalException(ErrorCode.FORBIDDEN_UPDATE_DOCUMENT);
        }

        applyPartialUpdate(document, request);

        Document saved = documentRepo.save(document);              // @PreUpdate

        return documentMapper.toDocumentUpdateResponse(saved);
    }


    // --------------------------------------------------------------------------------------------

    private boolean applyPartialUpdate(Document document, DocumentUpdateRequest request) {
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
        if (request.getVisibilityStatus() != null) {               // handled at deserialization
            document.setVisibilityStatus(request.getVisibilityStatus());
        } // else: keep current
        return true;
    }

    private boolean safeDeleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return true;
        }
        try {
            supabaseStorageService.deleteFile(fileUrl);
        } catch (Exception cleanupError) {
            log.error("Failed to clean up orphaned file after rollback: url={}, reason={}",
                    fileUrl, cleanupError.getMessage());
        }
        return true;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GlobalException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        throw new GlobalException(ErrorCode.UNAUTHENTICATED);
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
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );

            case "ppt", "pptx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            );

            case "xls", "xlsx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

}
>>>>>>> 3332934bb62768de309b293d55e2dfe268be2079
