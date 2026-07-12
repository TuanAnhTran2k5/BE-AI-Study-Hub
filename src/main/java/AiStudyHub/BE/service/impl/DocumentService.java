package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.constraint.UploadStatus;
import AiStudyHub.BE.constraint.ScoreTypeCode;
import AiStudyHub.BE.dto.Request.DocumentUpdateRequest;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.*;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.RagDocument;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.*;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.IDocument;
import AiStudyHub.BE.service.IUser;
import AiStudyHub.BE.service.IGamification;
import AiStudyHub.BE.service.IRagSystem;
import AiStudyHub.BE.service.IStorageService;
import AiStudyHub.BE.service.ISupabaseStorage;
import AiStudyHub.BE.service.IDuplicateCheck;
import AiStudyHub.BE.service.INotification;
import AiStudyHub.BE.utils.SimHashUtil;
import AiStudyHub.BE.utils.TextExtractionUtil;
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
    RatingRepo ratingRepo;
    BookmarkRepo bookmarkRepo;
    ReportRepo reportRepo;
    ReportCaseRepo reportCaseRepo;
    ScoreLogRepo scoreLogRepo;
    NotificationRepo notificationRepo;
    ChatSessionDocumentRepo chatSessionDocumentRepo;
    IUser userService;
    INotification notificationService;
    SimHashUtil simHashUtil;
    TextExtractionUtil textExtractionUtil;


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
        String originalFileName = request.getFile().getOriginalFilename();
        String contentType = request.getFile().getContentType();

        VisibilityStatus visibilityStatus =
                request.getVisibilityStatus() == null
                        ? VisibilityStatus.PRIVATE
                        : request.getVisibilityStatus();

        FileUploadResponse fileMetadata = null;
        try {
            // 1. Upload to Supabase
            fileMetadata = supabaseStorageService.uploadBytes(fileBytes, originalFileName, null, contentType);
            log.info("Uploaded file to Supabase successfully: {}", fileMetadata.getPublicUrl());

            // 2. Save document entity
            Document document = documentMapper.toDocument(request);
            document.setOwner(owner);
            document.setSubject(subject);
            document.setFileName(fileMetadata.getOriginalFileName());
            document.setFileUrl(fileMetadata.getPublicUrl());
            document.setFileType(fileMetadata.getContentType());
            document.setFileSize(fileMetadata.getFileSize());
            document.setVisibilityStatus(visibilityStatus);
            document.setModerationStatus(ModerationStatus.NORMAL);
            document.setUploadStatus(UploadStatus.UPLOADING);
            document.setAverageRating(0.0);
            document.setRatingCount(0);
            document.setDownloadCount(0);
            document.setBookmarkCount(0);
            document.setReportCount(0);

            document = documentRepo.save(document);

            // 3. Duplicate Check
            Document duplicatedDoc = duplicateCheckService.performDuplicateCheck(document.getDocumentId(), fileBytes);
            if (duplicatedDoc != null) {
                document.setVisibilityStatus(VisibilityStatus.PRIVATE);
                log.info("Duplication detected! Document set to PRIVATE. Duplicated with doc ID: {}", duplicatedDoc.getDocumentId());
            }

            // 4. Update user storage usage
            storageService.increaseStorage(owner, fileSize);
            userRepo.save(owner);

            // 5. Award badges / check achievements
            gamificationService.checkAndAwardBadges(owner.getUserId());

            // 6. Auto index RAG
            documentRagIndexer.autoIndexIfSupported(document, fileBytes);

            // 7. Update status to COMPLETED
            document.setUploadStatus(UploadStatus.COMPLETED);
            document = documentRepo.save(document);

            log.info("Upload process finished successfully for documentId: {}", document.getDocumentId());

            DocumentUploadResponse response = documentMapper.toDocumentUploadResponse(document);
            response.setMessage("Upload and process document successfully");
            try {
                UserResponse profile = userService.getProfile(owner.getUserId());
                response.setStorageUsed(profile.getStorageUsed());
                response.setStorageRemaining(profile.getStorageRemaining());
                response.setStorageUsagePercent(profile.getStorageUsagePercent());
            } catch (Exception ex) {
                log.warn("Failed to populate updated storage info in upload response: {}", ex.getMessage());
            }
            return response;

        } catch (Exception e) {
            log.error("Upload process failed", e);
            if (fileMetadata != null) {
                safeDeleteFile(fileMetadata.getPublicUrl());
            }
            throw e;
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

        // === BƯỚC 1: Xóa RAG resources (nếu có) ===
        RagDocument ragDoc = ragDocumentRepository.findByDocumentDocumentId(documentId).orElse(null);
        if (ragDoc != null) {
            log.info("Deleting RAG resources for document ID: {}", documentId);
            ragSystemService.deleteDocument(documentId);
        }

        // === BƯỚC 2: Xóa download record đúng cách ===
        if (document.getSourceDocument() != null) {
            // Đang xóa BẢN COPY → xóa download record trỏ vào tài liệu GỐC cho user hiện tại
            downloadRepo.deleteByUserUserIdAndDocumentDocumentId(
                currentUser.getUserId(),
                document.getSourceDocument().getDocumentId()
            );
        } else {
            // Đang xóa TÀI LIỆU GỐC → xóa tất cả download records trỏ vào nó
            downloadRepo.deleteByDocumentDocumentId(documentId);
        }

        // === BƯỚC 3: Xóa các quan hệ khác ===
        notificationRepo.deleteByDocumentDocumentId(documentId);
        ratingRepo.deleteByDocumentDocumentId(documentId);
        bookmarkRepo.deleteByDocumentDocumentId(documentId);
        chatSessionDocumentRepo.deleteByDocumentDocumentId(documentId);
        reportRepo.deleteByDocumentDocumentId(documentId);
        reportCaseRepo.deleteByDocumentDocumentId(documentId);

        // === BƯỚC 4: Xử lý score logs (giữ nguyên logic hiện tại) ===
        List<ScoreLog> docScoreLogs = scoreLogRepo.findByDocumentId(documentId);
        int totalDeduction = docScoreLogs.stream()
                .mapToInt(ScoreLog::getScoreChange)
                .sum();

        if (totalDeduction != 0) {
            long currentScore = owner.getTotalScore() == null ? 0L : owner.getTotalScore();
            owner.setTotalScore(currentScore - totalDeduction);
            userRepo.save(owner);

            gamificationService.addWeeklyScore(owner.getUserId(), -totalDeduction);
            gamificationService.updateUserRank(owner.getUserId());
        }
        if (!docScoreLogs.isEmpty()) {
            scoreLogRepo.deleteAll(docScoreLogs);
        }

        // === BƯỚC 5: Gỡ FK source_document_id từ các bản copy (chỉ khi xóa TÀI LIỆU GỐC) ===
        if (document.getSourceDocument() == null) {
            List<Document> copies = documentRepo.findBySourceDocumentDocumentId(documentId);
            for (Document copy : copies) {
                copy.setSourceDocument(null);
            }
            documentRepo.saveAll(copies);
        }

        // === BƯỚC 6: Xóa document ===
        long deletedRows = documentRepo.deleteByDocumentId(documentId);

        if (deletedRows == 0) {
            throw new GlobalException(ErrorCode.DOCUMENT_DELETE_FAILED);
        }

        storageService.decreaseStorage(owner, fileSize);
        userRepo.save(owner);
        supabaseStorageService.deleteFile(fileUrl);

        try {
            UserResponse profile = userService.getProfile(owner.getUserId());
            response.setStorageUsed(profile.getStorageUsed());
            response.setStorageRemaining(profile.getStorageRemaining());
            response.setStorageUsagePercent(profile.getStorageUsagePercent());
        } catch (Exception ex) {
            log.warn("Failed to populate updated storage info in delete response: {}", ex.getMessage());
        }

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

        if (document.getSourceDocument() != null) {
            throw new GlobalException(ErrorCode.CANNOT_EDIT_DOWNLOADED_DOCUMENT);
        }

        applyPartialUpdate(document, request);

        Document saved = documentRepo.save(document); // @PreUpdate

        return documentMapper.toDocumentUpdateResponse(saved);
    }

    @Override
    public List<DocumentResponse> searchDocumentsByTitle(String keyword) {
        return documentRepo.findByTitleContainingIgnoreCase(keyword).stream()
                .filter(doc -> doc.getVisibilityStatus() == VisibilityStatus.PUBLIC 
                        && doc.getUploadStatus() == UploadStatus.COMPLETED
                        && doc.getModerationStatus() == ModerationStatus.NORMAL
                        && doc.getDeletedAt() == null)
                .map(documentMapper::toDocumentResponse)
                .collect(Collectors.toList());
    }

    private DocumentResponse enrichDocumentResponse(Document doc) {
        DocumentResponse resp = documentMapper.toDocumentResponse(doc);
        Document source = doc.getSourceDocument();
        if (source != null) {
            // Đồng bộ stats từ tài liệu gốc
            resp.setDownloadCount(source.getDownloadCount());
            resp.setBookmarkCount(source.getBookmarkCount());
            resp.setAverageRating(source.getAverageRating());
            resp.setRatingCount(source.getRatingCount());
            
            // Gán thông tin uploader gốc
            User originalOwner = source.getOwner();
            if (originalOwner != null) {
                resp.setOriginalUploaderId(originalOwner.getUserId());
                resp.setOriginalUploaderName(originalOwner.getFullName());
                resp.setOriginalUploaderAvatar(originalOwner.getAvatarUrl());
                resp.setOwnerName(originalOwner.getFullName());
                resp.setOwnerAvatar(originalOwner.getAvatarUrl());
            }
        } else {
            // Tự upload hoặc tài liệu gốc đã bị xóa
            User owner = doc.getOwner();
            if (owner != null) {
                resp.setOwnerName(owner.getFullName());
                resp.setOwnerAvatar(owner.getAvatarUrl());
                resp.setOriginalUploaderId(owner.getUserId());
                resp.setOriginalUploaderName(owner.getFullName());
                resp.setOriginalUploaderAvatar(owner.getAvatarUrl());
            }
        }

        // Nếu tài liệu bị ẩn hoặc xóa do vi phạm, truy vấn email admin và lý do để owner kháng nghị
        if (doc.getModerationStatus() != ModerationStatus.NORMAL) {
            List<AiStudyHub.BE.entity.ReportCase> cases = reportCaseRepo.findByDocumentDocumentIdOrderByResolvedAtDesc(doc.getDocumentId());
            AiStudyHub.BE.entity.ReportCase latestResolvedCase = cases.stream()
                    .filter(c -> c.getCaseStatus() == AiStudyHub.BE.constraint.CaseStatus.RESOLVED && c.getResolvedBy() != null)
                    .findFirst()
                    .orElse(null);
            if (latestResolvedCase != null) {
                resp.setModeratedByEmail(latestResolvedCase.getResolvedBy().getEmail());
                resp.setModerationNote(latestResolvedCase.getAdminNote());
            }
        }

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(Long userId) {
        return documentRepo.findByOwnerUserId(userId).stream()
                .map(this::enrichDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentDetail(Long documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getDeletedAt() != null) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Kiểm tra quyền truy cập tài liệu bị ẩn/gỡ bỏ TRƯỚC KHI enrich response để tránh lộ dữ liệu
        if (document.getModerationStatus() != ModerationStatus.NORMAL) {
            User currentUser = null;
            try {
                currentUser = SecurityUtils.getCurrentUser();
            } catch (Exception ignored) {}

            if (currentUser == null || 
                (!currentUser.getRole().equals(AiStudyHub.BE.constraint.UserRole.AD) 
                 && !document.getOwner().getUserId().equals(currentUser.getUserId()))) {
                throw new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND);
            }
        }

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

        DocumentResponse response = enrichDocumentResponse(document);

        // Populate isBookmarked, myRating if the user is authenticated
        try {
            User currentUser = SecurityUtils.getCurrentUser();
            if (currentUser != null && currentUser.getUserId() != null) {
                Long targetDocId = document.getSourceDocument() != null ? document.getSourceDocument().getDocumentId() : documentId;

                boolean isBookmarked = bookmarkRepo.existsByUserUserIdAndDocumentDocumentId(currentUser.getUserId(), targetDocId)
                        || bookmarkRepo.existsByUserUserIdAndDocumentDocumentId(currentUser.getUserId(), documentId);
                response.setIsBookmarked(isBookmarked);

                ratingRepo.findByUserUserIdAndDocumentDocumentId(currentUser.getUserId(), targetDocId)
                        .ifPresent(rating -> response.setMyRating(rating.getRatingValue()));
            }
        } catch (Exception e) {
            log.debug("Unauthenticated user accessing document detail: {}", e.getMessage());
        }

        return response;
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

        // Block owner from downloading their own document
        if (publicOwner.getUserId().equals(currentUser.getUserId())) {
            throw new GlobalException(ErrorCode.CANNOT_DOWNLOAD_OWN_DOCUMENT);
        }

        // Early duplicate check (UX layer — clean error before any file I/O)
        if (downloadRepo.existsByUserAndDocument(currentUser, publicDocument)) {
            throw new GlobalException(ErrorCode.DOCUMENT_ALREADY_DOWNLOADED);
        }

        byte[] fileBytes = supabaseStorageService.downloadFile(publicDocument.getFileUrl());

        storageService.validateStorage(currentUser, fileBytes.length);

        String personalFolder = "users/" + currentUser.getUserId() + "/documents";

        FileUploadResponse uploadResponse = supabaseStorageService.uploadBytes(
                fileBytes,
                publicDocument.getFileName(),
                personalFolder,
                publicDocument.getFileType()
        );

        try {
            String resolvedSimHash = publicDocument.getSimHashContent();
            if (resolvedSimHash == null) {
                try {
                    String text = textExtractionUtil.extractText(
                            new java.io.ByteArrayInputStream(fileBytes));
                    if (text != null && !text.trim().isEmpty()) {
                        resolvedSimHash = simHashUtil.calculateSimHash(text);
                    }
                } catch (Exception hashEx) {
                    log.warn("Could not compute simHash for download copy of documentId={}: {}",
                            documentId, hashEx.getMessage());
                }
            }

            Document privateDocument = Document.builder()
                    .owner(currentUser)
                    .subject(publicDocument.getSubject())
                    .title(publicDocument.getTitle())
                    .visibilityStatus(VisibilityStatus.PRIVATE)
                    .moderationStatus(publicDocument.getModerationStatus())
                    .averageRating(0.0)
                    .fileName(publicDocument.getFileName())
                    .fileUrl(uploadResponse.getPublicUrl())
                    .fileType(uploadResponse.getContentType())
                    .fileSize(uploadResponse.getFileSize())
                    .simHashContent(resolvedSimHash)
                    .sourceDocument(publicDocument)
                    .build();

            privateDocument = documentRepo.save(privateDocument);
            storageService.increaseStorage(currentUser, uploadResponse.getFileSize());
            userRepo.save(currentUser);

            // Index the downloaded copy into Qdrant so the owner can use AI chat on it.
            // Re-use the already-downloaded fileBytes — no extra network call needed.
            documentRagIndexer.autoIndexIfSupported(privateDocument, fileBytes);

            // Mark as COMPLETED after RAG indexing (mirrors the uploadDocument flow).
            privateDocument.setUploadStatus(UploadStatus.COMPLETED);
            privateDocument = documentRepo.save(privateDocument);

            boolean isFirstTimeEver = !scoreLogRepo.existsByActorUserIdAndDocumentIdAndScoreTypeTypeCode(
                    currentUser.getUserId(),
                    publicDocument.getDocumentId(),
                    ScoreTypeCode.DOC_DOWNLOAD.name()
            );

            Integer addedPoint = 0;

            if (isFirstTimeEver) {
                publicDocument.setDownloadCount(
                        publicDocument.getDownloadCount() == null ? 1 : publicDocument.getDownloadCount() + 1
                );
                documentRepo.save(publicDocument);
            }

            Download download = Download.builder()
                    .user(currentUser)
                    .document(publicDocument)
                    .scoreAwarded(false)
                    .build();

            if (isFirstTimeEver) {
                // Owner check already done above, so publicOwner != currentUser here
                int points = gamificationService.getPoints(ScoreTypeCode.DOC_DOWNLOAD.name(), 5);
                String desc = "Awarded " + points + " points because " + currentUser.getFullName()
                                + " downloaded your document: " + publicDocument.getTitle();
                addedPoint = gamificationService.awardScore(ScoreContextResponse.builder()
                        .receiverUserId(publicOwner.getUserId())
                        .documentId(publicDocument.getDocumentId())
                        .documentTitle(publicDocument.getTitle())
                        .spec(new IGamification.ScoreTypeSpec(ScoreTypeCode.DOC_DOWNLOAD.name(), "Document Download", points,
                                "Score awarded when another user downloads your document"))
                        .scoreChange(points)
                        .description(desc)
                        .actorUserId(currentUser.getUserId())
                        .build());

                download.setScoreAwarded(true);

                // Re-evaluate rank due to score change
                gamificationService.updateUserRank(publicOwner.getUserId());
                gamificationService.addWeeklyScore(publicOwner.getUserId(), addedPoint);

                if (addedPoint != null && addedPoint > 0) {
                    notificationService.sendDocumentDownloadNotification(publicOwner, currentUser, publicDocument, addedPoint);
                }
            }

            downloadRepo.save(download);

            // Re-evaluate badges due to download count increase
            gamificationService.checkAndAwardBadges(publicOwner.getUserId());

            DocumentDownloadResponse response = documentMapper.toDocumentDownloadResponse(
                    privateDocument,
                    true,
                    addedPoint,
                    publicOwner.getTotalScore(),
                    LocalDateTime.now()
            );

            response.setPublicOwnerName(publicOwnerName);

            response.setOriginalUploaderId(publicOwner.getUserId());
            response.setOriginalUploaderName(publicOwner.getFullName());
            response.setOriginalUploaderAvatar(publicOwner.getAvatarUrl());

            try {
                UserResponse ownerProfile = userService.getProfile(publicOwner.getUserId());
                response.setOwnerCurrentRank(ownerProfile.getCurrentRank());
                if (ownerProfile.getRankProgress() != null) {
                    response.setOwnerNextRank(ownerProfile.getRankProgress().getNextRank());
                    response.setOwnerProgressPercent(ownerProfile.getRankProgress().getProgressPercent());
                }
            } catch (Exception ex) {
                log.warn("Failed to populate owner rank progress in download response: {}", ex.getMessage());
            }

            return response;

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Race condition: unique constraint caught duplicate — rollback + cleanup
            safeDeleteFile(uploadResponse.getPublicUrl());
            throw new GlobalException(ErrorCode.DOCUMENT_ALREADY_DOWNLOADED);
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
        // title: null, blank, or Swagger default placeholder "string" → skip
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty() && !request.getTitle().equals("string")) {
            document.setTitle(request.getTitle().trim());
        }

        // subjectId: null or <= 0 → skip (FE sometimes sends 0 as "no change")
        if (request.getSubjectId() != null && request.getSubjectId() > 0) {
            Subject subject = subjectRepo.findById(request.getSubjectId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.SUBJECT_NOT_FOUND));
            document.setSubject(subject);
        }

        // visibilityStatus: null → skip
        if (request.getVisibilityStatus() != null) {
            VisibilityStatus newStatus = request.getVisibilityStatus();

            if (newStatus == VisibilityStatus.PUBLIC
                    && document.getVisibilityStatus() == VisibilityStatus.PRIVATE
                    && document.getSimHashContent() != null) {

                boolean stillDuplicate = documentRepo
                        .findByOwnerUserId(document.getOwner().getUserId())
                        .stream()
                        .filter(d -> d.getSimHashContent() != null)
                        .filter(d -> d.getUploadStatus() == UploadStatus.COMPLETED)
                        .filter(d -> !d.getDocumentId().equals(document.getDocumentId()))
                        .anyMatch(d -> simHashUtil.calculateHammingDistance(
                                document.getSimHashContent(), d.getSimHashContent()) <= 3);

                if (stillDuplicate) {
                    throw new GlobalException(400,
                            "Cannot make document public: it is a duplicate of an existing public document.");
                }
            }

            document.setVisibilityStatus(newStatus);
        }
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