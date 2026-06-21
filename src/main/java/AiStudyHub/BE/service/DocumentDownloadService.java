package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.DownloadRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.security.SecurityUtils;
import AiStudyHub.BE.service.impl.IDocumentDownload;
import AiStudyHub.BE.service.impl.IRankingBadgeService;
import AiStudyHub.BE.service.impl.IScore;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentDownloadService implements IDocumentDownload {

    DocumentRepo documentRepo;
    UserRepo userRepo;
    DownloadRepo downloadRepo;
    DocumentMapper documentMapper;
    IStorageService storageService;
    ISupabaseStorage supabaseStorageService;
    IScore scoreService;
    IRankingBadgeService rankingBadgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception {
        Document publicDocument = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (publicDocument.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
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
                    addedPoint = scoreService.awardScore(
                            publicOwner,
                            publicDocument,
                            new IScore.ScoreTypeSpec("DOC_DOWNLOAD", "Document Download", 5,
                                    "Score awarded when another user downloads your document"),
                            5,
                            "Awarded 5 points because " + currentUser.getFullName()
                                    + " downloaded your document: " + publicDocument.getTitle());

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

    // --------------------------------------------------------------------------------------------

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
