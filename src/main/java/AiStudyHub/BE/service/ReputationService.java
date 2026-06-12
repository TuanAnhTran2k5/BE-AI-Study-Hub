package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ReputationService {

    static final String RATING_REPUTATION = "RATING_REPUTATION";

    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ScoreTypeRepo scoreTypeRepo;

    @Autowired
    private ScoreLogRepo scoreLogRepo;

    // Self-reference so that applyReputation(Document) is invoked through the Spring proxy.
    // This keeps each per-document call inside its own @Transactional boundary, giving the
    // fault isolation required (a failed document does not roll back already-committed
    // documents).
    @Autowired
    @Lazy
    private ReputationService self;

    // Scheduled entry point. Processes each eligible document, isolating failures per document so a
    // single failure does not stop the job. Returns the number of documents processed successfully.
    @Scheduled(cron = "${reputation.job.cron:0 0 2 * * *}")
    public int runDailyReputation() {
        List<Document> docs = documentRepo.findEligibleForReputation(VisibilityStatus.PUBLIC, 10);

        log.info("Reputation job started: {} eligible document(s)", docs.size());

        int processed = 0;
        for (Document doc : docs) {
            try {
                self.applyReputation(doc.getDocumentId());  // per-document transaction via proxy
                processed++;
            } catch (Exception ex) {
                log.error("Reputation failed for documentId={}: {}", doc.getDocumentId(), ex.getMessage(), ex);
                // continue with the next document
            }
        }

        log.info("Reputation job finished: {}/{} document(s) processed", processed, docs.size());
        return processed;
    }

    // Applies the reputation threshold table for a single document inside its own transaction.
    // Loads the document by id within the transaction so its lazy owner can be initialized.
    // A document is eligible if it has reached the rating threshold at least once (sticky flag)
    // or currently has ratingCount >= 10; once eligible it stays eligible (flag is set true).
    // Returns true if a reputation change was applied, false if the document did not qualify.
    @Transactional(rollbackFor = Exception.class)
    public boolean applyReputation(Long documentId) {
        Document doc = documentRepo.findById(documentId).orElse(null);
        if (doc == null) {
            return false;
        }

        int count = doc.getRatingCount() == null ? 0 : doc.getRatingCount();
        boolean eligible = Boolean.TRUE.equals(doc.getRatingThresholdReached()) || count >= 10;
        if (!eligible) {
            return false;
        }

        // Sticky: once the document qualifies, keep it eligible for future runs.
        if (!Boolean.TRUE.equals(doc.getRatingThresholdReached())) {
            doc.setRatingThresholdReached(true);
            documentRepo.save(doc);
        }

        double avg = doc.getAverageRating() == null ? 0.0 : doc.getAverageRating();
        int change = ReputationPolicy.tierScore(avg, count);

        User owner = doc.getOwner();
        long current = owner.getTotalScore() == null ? 0L : owner.getTotalScore();
        owner.setTotalScore(current + change);            // accumulate
        userRepo.save(owner);

        ScoreType type = scoreTypeRepo.findByTypeCode(RATING_REPUTATION)
                .orElseGet(() -> scoreTypeRepo.save(ScoreType.builder()
                        .typeCode(RATING_REPUTATION)
                        .typeName("Rating Reputation")
                        .defaultPoint(0)
                        .description("Daily reputation score derived from document ratings")
                        .build()));                       // find-or-create

        scoreLogRepo.save(ScoreLog.builder()
                .user(owner)
                .document(doc)
                .scoreType(type)
                .scoreChange(change)
                .description("Reputation " + (change >= 0 ? "+" : "") + change
                        + " for avg=" + avg + ", count=" + count)
                .build());

        return true;
    }
}
