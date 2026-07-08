package AiStudyHub.BE.config;

import AiStudyHub.BE.constraint.ReportSeverity;
import AiStudyHub.BE.entity.ReportReason;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.repository.ReportReasonRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportSystemInitializer implements CommandLineRunner {

    private final ScoreTypeRepo scoreTypeRepo;
    private final ReportReasonRepo reportReasonRepo;

    @Override
    public void run(String... args) throws Exception {
        log.info("ReportSystemInitializer checking default ScoreTypes...");

        if (!scoreTypeRepo.findByTypeCode("REPORT_PENALTY").isPresent()) {
            ScoreType reportPenalty = ScoreType.builder()
                    .typeCode("REPORT_PENALTY")
                    .typeName("Report Penalty")
                    .defaultPoint(0)
                    .description("Penalty points deducted for document copyright or content violations")
                    .build();
            scoreTypeRepo.save(reportPenalty);
            log.info("Seeded REPORT_PENALTY ScoreType.");
        }

        if (!scoreTypeRepo.findByTypeCode("FALSE_REPORT_PENALTY").isPresent()) {
            ScoreType falseReportPenalty = ScoreType.builder()
                    .typeCode("FALSE_REPORT_PENALTY")
                    .typeName("False Report Penalty")
                    .defaultPoint(0)
                    .description("Penalty points deducted for submitting fake or spam reports")
                    .build();
            scoreTypeRepo.save(falseReportPenalty);
            log.info("Seeded FALSE_REPORT_PENALTY ScoreType.");
        }

        log.info("ReportSystemInitializer checking default ReportReasons...");
        if (reportReasonRepo.count() == 0) {
            reportReasonRepo.save(ReportReason.builder()
                    .reasonName("Spam or Advertisement")
                    .severityLevel(ReportSeverity.LOW)
                    .description("Spam content, double uploads, or commercial advertising.")
                    .reportThreshold(10)
                    .penaltyScore(2)
                    .build());

            reportReasonRepo.save(ReportReason.builder()
                    .reasonName("Wrong Subject or Inaccurate Content")
                    .severityLevel(ReportSeverity.MEDIUM)
                    .description("Document uploaded under the wrong course/subject, or has highly incorrect information.")
                    .reportThreshold(5)
                    .penaltyScore(5)
                    .build());

            reportReasonRepo.save(ReportReason.builder()
                    .reasonName("Copyright Violation")
                    .severityLevel(ReportSeverity.HIGH)
                    .description("Intellectual property theft or sharing copyrighted study material without authorization.")
                    .reportThreshold(3)
                    .penaltyScore(10)
                    .build());

            reportReasonRepo.save(ReportReason.builder()
                    .reasonName("Offensive or Inappropriate Content")
                    .severityLevel(ReportSeverity.HIGH)
                    .description("Contains harassment, offensive language, or inappropriate study materials.")
                    .reportThreshold(3)
                    .penaltyScore(15)
                    .build());

            log.info("Seeded 4 default ReportReasons successfully.");
        }
    }
}
