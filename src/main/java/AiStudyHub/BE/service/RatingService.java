package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.constraint.VisibilityStatus;
import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.RatingResponse;
import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.Rating;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.RatingMapper;
import AiStudyHub.BE.repository.DocumentRepo;
import AiStudyHub.BE.repository.RatingRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IRating;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class RatingService implements IRating {

    @Autowired
    private RatingRepo ratingRepo;
    @Autowired
    private DocumentRepo documentRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private RatingMapper ratingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RatingResponse submitRating(Long documentId, RatingRequest request) {
        // 1. Authentication
        User authUser = getCurrentUser();

        // 2. Validate input value: null -> FIELD_REQUIRED; range 1..5 -> INVALID_RATING_VALUE
        Integer ratingValue = request.getRatingValue();
        if (ratingValue == null) {
            throw new GlobalException(ErrorCode.FIELD_REQUIRED);
        }
        if (ratingValue < 1 || ratingValue > 5) {
            throw new GlobalException(ErrorCode.INVALID_RATING_VALUE);
        }

        // 3. Load document
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 4. Must be PUBLIC
        if (document.getVisibilityStatus() != VisibilityStatus.PUBLIC) {
            throw new GlobalException(ErrorCode.DOCUMENT_NOT_PUBLIC);
        }

        // 5. Load user from DB
        User user = userRepo.findById(authUser.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 6. Owner cannot rate own document
        if (document.getOwner().getUserId().equals(user.getUserId())) {
            throw new GlobalException(ErrorCode.CANNOT_RATE_OWN_DOCUMENT);
        }

        // 7. Create vs update
        Rating rating = ratingRepo.findByUserAndDocument(user, document)
                .orElseGet(() -> Rating.builder()
                        .user(user)
                        .document(document)
                        .build());
        rating.setRatingValue(ratingValue);
        rating.setComment(request.getComment());
        rating = ratingRepo.save(rating);

        // 8. Recompute aggregates and persist Document in the same transaction
        recomputeAggregates(document);

        // 9. Build response
        return ratingMapper.toRatingResponse(rating, document);
    }

    // --------------------------------------------------------------------------------------------

    // Recompute ratingCount and averageRating of a document from the rating table,
    // then persist the document within the current transaction.
    // Recompute ratingCount and averageRating of a document from the rating table,
    // then persist the document within the current transaction. Returns the persisted document.
    private Document recomputeAggregates(Document document) {
        java.util.List<AiStudyHub.BE.entity.Rating> ratings = ratingRepo.findByDocument(document);
        int count = ratings.size();

        if (count == 0) {
            document.setRatingCount(0);
            document.setAverageRating(0.0);
        } else {
            double avg = ratings.stream().mapToDouble(AiStudyHub.BE.entity.Rating::getRatingValue).average().orElse(0.0);
            document.setRatingCount(count);
            document.setAverageRating(round2(avg));
            // Once the document reaches the rating threshold, mark it eligible for reputation
            // scoring permanently (sticky) so it keeps being scored even if count later drops.
            if (count >= 10) {
                document.setRatingThresholdReached(true);
            }
        }

        return documentRepo.save(document);
    }

    // Pure helper: round to 2 decimal places using HALF_UP.
    private static double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
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
}
