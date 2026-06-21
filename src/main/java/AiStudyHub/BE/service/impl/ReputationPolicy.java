package AiStudyHub.BE.service.impl;


public final class ReputationPolicy {

    private ReputationPolicy() {
    }

    // Pure tier mapping based on averageRating (ratingCount only splits +10 vs +8).
    // Eligibility (whether the document should be scored at all) is decided by the caller
    // via the sticky Document.ratingThresholdReached flag, not here.
    public static int tierScore(double averageRating, int ratingCount) {
        if (averageRating >= 4.5) {
            return ratingCount >= 20 ? 10 : 8;
        }
        if (averageRating >= 4.0) {
            return 5;
        }
        if (averageRating >= 3.0) {
            return 2;
        }
        if (averageRating >= 2.0) {
            return 1;
        }
        return -5;
    }
}
