package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.dto.Response.DocumentUploadResponse;
import AiStudyHub.BE.dto.Response.FileUploadResponse;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.mapper.DocumentMapper;
import AiStudyHub.BE.service.impl.IDocument;
import AiStudyHub.BE.service.impl.IStorageService;
import AiStudyHub.BE.service.impl.ISupabaseStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
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

    private String hashSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
