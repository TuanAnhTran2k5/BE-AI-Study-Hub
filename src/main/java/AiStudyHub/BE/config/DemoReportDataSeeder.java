package AiStudyHub.BE.config;

import AiStudyHub.BE.constraint.*;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

/**
 * DEMO SEEDER FILE - SAFE TO DELETE AFTER LIVE DEMO PRESENTATION
 * Note: Annotated with @Profile("!prod") so it only runs in dev/test/demo environments.
 */
@Component
@Profile("!prod")
@Slf4j
@RequiredArgsConstructor
public class DemoReportDataSeeder implements CommandLineRunner {

    private final UserRepo userRepo;
    private final DocumentRepo documentRepo;
    private final SubjectRepo subjectRepo;
    private final ReportReasonRepo reportReasonRepo;
    private final ReportCaseRepo reportCaseRepo;
    private final ReportRepo reportRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (documentRepo.findByTitle("SWP391 Demo Syllabus & Software Development Plan.pdf").isPresent()) {
            log.info("Demo Report Data already seeded. Skipping.");
            return;
        }

        log.info("Seeding Demo Report Data for Live Demo Presentation...");

        String password = passwordEncoder.encode("123456");

        // 1. Seed Demo Admin Users
        User admin1 = userRepo.findByEmail("admin1@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("admin1@studyhub.com")
                        .fullName("Admin One (Demo)")
                        .passwordHash(password)
                        .role(UserRole.AD)
                        .status(UserStatus.ACTIVE)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        User admin2 = userRepo.findByEmail("admin2@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("admin2@studyhub.com")
                        .fullName("Admin Two (Demo)")
                        .passwordHash(password)
                        .role(UserRole.AD)
                        .status(UserStatus.ACTIVE)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        // 2. Seed Demo Author & Reporter Users
        User author = userRepo.findByEmail("author_demo@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("author_demo@studyhub.com")
                        .fullName("Dang Anh Quoc (Author)")
                        .passwordHash(password)
                        .role(UserRole.US)
                        .status(UserStatus.ACTIVE)
                        .totalScore(100L)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        User reporter1 = userRepo.findByEmail("reporter1@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("reporter1@studyhub.com")
                        .fullName("Student Reporter 1")
                        .passwordHash(password)
                        .role(UserRole.US)
                        .status(UserStatus.ACTIVE)
                        .totalScore(50L)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        User reporter2 = userRepo.findByEmail("reporter2@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("reporter2@studyhub.com")
                        .fullName("Student Reporter 2")
                        .passwordHash(password)
                        .role(UserRole.US)
                        .status(UserStatus.ACTIVE)
                        .totalScore(50L)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        User reporter3 = userRepo.findByEmail("reporter3@studyhub.com").orElseGet(() ->
                userRepo.save(User.builder()
                        .email("reporter3@studyhub.com")
                        .fullName("Student Reporter 3")
                        .passwordHash(password)
                        .role(UserRole.US)
                        .status(UserStatus.ACTIVE)
                        .totalScore(50L)
                        .authProvider(AuthProvider.LOCAL)
                        .build())
        );

        Subject defaultSubject = subjectRepo.findAll().stream().findFirst().orElse(null);

        ReportReason lowReason = reportReasonRepo.findAll().stream()
                .filter(r -> r.getSeverityLevel() == ReportSeverity.LOW)
                .findFirst()
                .orElseGet(() -> reportReasonRepo.save(ReportReason.builder()
                        .reasonName("Spam or Advertisement")
                        .severityLevel(ReportSeverity.LOW)
                        .description("Spam content or double uploads")
                        .reportThreshold(3)
                        .penaltyScore(10)
                        .build()));

        // --- SCENARIO 1 DATA: Document A (W1 -> ready for 4th report to trigger W2) ---
        Document docA = documentRepo.save(Document.builder()
                .owner(author)
                .subject(defaultSubject)
                .title("SWP391 Demo Syllabus & Software Development Plan.pdf")
                .fileName("compressed.tracemonkey-pldi-09.pdf")
                .fileType("pdf")
                .fileSize(1024L * 50)
                .fileUrl("https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.NORMAL)
                .reportCount(3)
                .downloadCount(20)
                .build());

        ReportCase caseA = reportCaseRepo.save(ReportCase.builder()
                .document(docA)
                .reason(lowReason)
                .caseLevel(ReportSeverity.LOW)
                .reportCount(3)
                .requiredThreshold(3)
                .caseStatus(CaseStatus.WARNING_1)
                .openedAt(LocalDateTime.now().minusHours(2))
                .firstWarningAt(LocalDateTime.now().minusHours(1))
                .build());

        reportRepo.save(Report.builder().reporter(reporter1).document(docA).reason(lowReason).reportCase(caseA).description("Report 1 for Doc A").status(ReportStatus.PENDING).build());
        reportRepo.save(Report.builder().reporter(reporter2).document(docA).reason(lowReason).reportCase(caseA).description("Report 2 for Doc A").status(ReportStatus.PENDING).build());
        reportRepo.save(Report.builder().reporter(reporter3).document(docA).reason(lowReason).reportCase(caseA).description("Report 3 for Doc A").status(ReportStatus.PENDING).build());

        // --- SCENARIO 2 DATA: Document B (Ready for live High Severity report) ---
        Document docB = documentRepo.save(Document.builder()
                .owner(author)
                .subject(defaultSubject)
                .title("SWP391 Exam Cheating Answers & Leaked Paper.pdf")
                .fileName("compressed.tracemonkey-pldi-09.pdf")
                .fileType("pdf")
                .fileSize(1024L * 80)
                .fileUrl("https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.NORMAL)
                .reportCount(0)
                .build());

        // --- SCENARIO 3 DATA: Document C (Pending Case ready for Admin Claim & Resolve) ---
        Document docC = documentRepo.save(Document.builder()
                .owner(author)
                .subject(defaultSubject)
                .title("PRN231 Advanced C# Project Source Code.zip")
                .fileName("sample.zip")
                .fileType("zip")
                .fileSize(1024L * 500)
                .fileUrl("https://example.com/sample.zip")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.NORMAL)
                .reportCount(1)
                .build());

        ReportCase caseC = reportCaseRepo.save(ReportCase.builder()
                .document(docC)
                .reason(lowReason)
                .caseLevel(ReportSeverity.LOW)
                .reportCount(1)
                .requiredThreshold(3)
                .caseStatus(CaseStatus.PENDING_REVIEW)
                .openedAt(LocalDateTime.now().minusHours(3))
                .build());

        reportRepo.save(Report.builder().reporter(reporter1).document(docC).reason(lowReason).reportCase(caseC).description("Sample report for Admin Claim demo").status(ReportStatus.PENDING).build());

        // --- SCENARIO 4 & 5 DATA: Document D (Resolved Case ready for Admin Refund Appeal) ---
        Document docD = documentRepo.save(Document.builder()
                .owner(author)
                .subject(defaultSubject)
                .title("MAS291 Probability and Statistics Cheat Sheet.pdf")
                .fileName("compressed.tracemonkey-pldi-09.pdf")
                .fileType("pdf")
                .fileSize(1024L * 120)
                .fileUrl("https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .uploadStatus(UploadStatus.COMPLETED)
                .moderationStatus(ModerationStatus.HIDDEN)
                .reportCount(3)
                .build());

        ReportCase caseD = reportCaseRepo.save(ReportCase.builder()
                .document(docD)
                .reason(lowReason)
                .caseLevel(ReportSeverity.LOW)
                .reportCount(3)
                .requiredThreshold(3)
                .caseStatus(CaseStatus.RESOLVED)
                .openedAt(LocalDateTime.now().minusDays(1))
                .firstWarningAt(LocalDateTime.now().minusDays(1))
                .resolvedAt(LocalDateTime.now().minusHours(5))
                .resolvedBy(admin1)
                .adminNote("Penalized for initial report check")
                .build());

        reportRepo.save(Report.builder().reporter(reporter2).document(docD).reason(lowReason).reportCase(caseD).description("Report for MAS291 cheat sheet").status(ReportStatus.RESOLVED).build());

        log.info("Demo Report Data seeded successfully! Ready for Live Demo Presentation.");
    }
}
