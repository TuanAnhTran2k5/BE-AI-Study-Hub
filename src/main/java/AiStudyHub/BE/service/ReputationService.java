package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.entity.*;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RatingRepo;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IReputation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ReputationService implements IReputation {

    static final String RATING_REPUTATION = "RATING_REPUTATION";

    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ScoreTypeRepo scoreTypeRepo;

    @Autowired
    private ScoreLogRepo scoreLogRepo;

    @Autowired
    private RatingRepo ratingRepo;

    @Autowired
    private AiStudyHub.BE.service.impl.IRankingBadgeService rankingBadgeService;

    @Autowired
    @Lazy
    private IReputation self;

    @Override
    @Scheduled(cron = "${reputation.job.cron:0 0 2 * * *}")
    public int runDailyReputation() {
        self.recomputeAllAggregates(); // Recompute ratings for all documents first

        List<Document> docs = documentRepo.findByVisibilityStatusAndRatingCountGreaterThanEqual(VisibilityStatus.PUBLIC, 10);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyReputation(Long documentId) {
        Document doc = documentRepo.findById(documentId).orElse(null);
        if (doc == null) {
            return false;
        }

        int count = doc.getRatingCount() == null ? 0 : doc.getRatingCount();
        boolean eligible = count >= 10;
        if (!eligible) {
            return false;
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

        rankingBadgeService.updateUserRank(owner.getUserId());
        rankingBadgeService.addWeeklyScore(owner.getUserId(), change);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recomputeAllAggregates() {
        List<Document> docsWithRatings = documentRepo.findByVisibilityStatus(VisibilityStatus.PUBLIC);
        for (Document doc : docsWithRatings) {
            List<Rating> ratings = ratingRepo.findByDocument(doc);
            int count = ratings.size();
            if (count == 0) {
                doc.setRatingCount(0);
                doc.setAverageRating(0.0);
            } else {
                double avg = ratings.stream().mapToDouble(Rating::getRatingValue).average().orElse(0.0);
                doc.setRatingCount(count);
                doc.setAverageRating(round2(avg));
            }
            documentRepo.save(doc);
        }
        return true;
    }

    //---------------------------------------------------------------------------

    private static double round2(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
