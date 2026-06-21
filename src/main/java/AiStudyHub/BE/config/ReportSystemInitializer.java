package AiStudyHub.BE.config;

import AiStudyHub.BE.entity.ScoreType;
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
    }
}
