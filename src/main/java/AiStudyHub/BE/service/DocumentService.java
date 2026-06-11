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
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentDeleteResponse;
import AiStudyHub.BE.dto.Response.DocumentDownloadResponse;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Download;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.repository.DownloadRepo;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.service.impl.IDocument;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class DocumentService implements IDocument {

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

    @Override
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

        return documentMapper.toDocumentUploadResponse(document);
    }

    @Override
    @Transactional
    public DocumentDeleteResponse deleteDocument(Long documentId) throws Exception {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        DocumentDeleteResponse response = documentMapper.toDocumentDeleteResponse(
                document,
                LocalDateTime.now()
        );

        supabaseStorageService.deleteFile(document.getFileUrl());

        long deletedRows = documentRepo.deleteByDocumentId(documentId);

        if (deletedRows == 0) {
            throw new GlobalException(ErrorCode.DOCUMENT_DELETE_FAILED);
        }

        return response;
    }

    @Override
    public DocumentDownloadResponse downloadPublicDocument(Long documentId) throws Exception {
        Document publicDocument = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (publicDocument.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
        }

        User currentUser = getCurrentUser();

        User publicOwner = publicDocument.getOwner();
        String publicOwnerName = publicOwner.getFullName();

        byte[] fileBytes = supabaseStorageService.downloadFile(publicDocument.getFileUrl());

        String personalFolder = "users/" + currentUser.getUserId() + "/documents";

        FileUploadResponse uploadResponse = supabaseStorageService.uploadBytes(
                fileBytes,
                publicDocument.getFileName(),
                personalFolder,
                publicDocument.getFileType()
        );

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
                .build();

        privateDocument = documentRepo.save(privateDocument);

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
            }
        }
        
        downloadRepo.save(download);

        DocumentDownloadResponse response = documentMapper.toDocumentDownloadResponse(
                privateDocument,
                firstDownload,
                addedPoint,
                publicOwner.getTotalScore(),
                LocalDateTime.now()
        );

        response.setPublicOwnerName(publicOwnerName);

        return response;
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


    // ----------------------------------------------------------------------
    private String hashSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
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
