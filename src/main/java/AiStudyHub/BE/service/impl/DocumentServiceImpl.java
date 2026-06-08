package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.constraint.ModerationStatus;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Subject;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.SubjectRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.DocumentService;
import AiStudyHub.BE.service.SupabaseStoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepo documentRepo;
    private final UserRepo userRepo;
    private final SubjectRepo subjectRepo;
    private final SupabaseStoreService supabaseStoreService;

    public DocumentServiceImpl(
            DocumentRepo documentRepo,
            UserRepo userRepo,
            SubjectRepo subjectRepo,
            SupabaseStoreService supabaseStoreService
    ) {
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.supabaseStoreService = supabaseStoreService;
    }

    @Override
    public Document uploadDocument(
            MultipartFile file,
            String title,
            Long ownerId,
            Long subjectId,
            VisibilityStatus visibilityStatus
    ) throws Exception {

        User owner = userRepo.findById(ownerId)
                             .orElseThrow(() -> new RuntimeException("User not found"));

        Subject subject = subjectRepo.findById(subjectId)
                                     .orElseThrow(() -> new RuntimeException("Subject not found"));

        byte[] fileBytes = file.getBytes();

        String fileUrl = supabaseStoreService.uploadFile(
                file.getOriginalFilename(),
                fileBytes,
                file.getContentType()
        );

        Document document = Document.builder()
                                    .owner(owner)
                                    .subject(subject)
                                    .title(title)
                                    .fileName(file.getOriginalFilename())
                                    .fileUrl(fileUrl)
                                    .fileType(file.getContentType())
                                    .fileSize(file.getSize())
                                    .contentHashSha256(hashSha256(fileBytes))
                                    .visibilityStatus(visibilityStatus == null ? VisibilityStatus.PRIVATE : visibilityStatus)
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