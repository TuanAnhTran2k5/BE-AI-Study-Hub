package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.DocumentUploadRequest;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IDocument;
import AiStudyHub.BE.service.SupabaseStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private SupabaseStoreService supabaseStoreService;

    @Override
    public Document uploadDocument(
            MultipartFile file,
            DocumentUploadRequest request
    ) throws Exception {

        User owner = userRepo.findById(request.getOwnerId())
                             .orElseThrow(() -> new RuntimeException("User not found"));

        Subject subject = subjectRepo.findById(request.getSubjectId())
                                     .orElseThrow(() -> new RuntimeException("Subject not found"));

        byte[] fileBytes = file.getBytes();

        String fileUrl = supabaseStoreService.uploadFile(
                file.getOriginalFilename(),
                fileBytes,
                file.getContentType()
        );

        VisibilityStatus visibilityStatus =
                request.getVisibilityStatus() == null
                        ? VisibilityStatus.PRIVATE
                        : request.getVisibilityStatus();

        Document document = Document.builder()
                                    .owner(owner)
                                    .subject(subject)
                                    .title(request.getTitle())
                                    .fileName(file.getOriginalFilename())
                                    .fileUrl(fileUrl)
                                    .fileType(file.getContentType())
                                    .fileSize(file.getSize())
                                    .contentHashSha256(hashSha256(fileBytes))
                                    .visibilityStatus(visibilityStatus)
                                    .moderationStatus(ModerationStatus.NORMAL)
                                    .averageRating(0.0)
                                    .ratingCount(0)
                                    .downloadCount(0)
                                    .bookmarkCount(0)
                                    .reportCount(0)
                                    .build();

        return documentRepo.save(document);
    }

    private String hashSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
