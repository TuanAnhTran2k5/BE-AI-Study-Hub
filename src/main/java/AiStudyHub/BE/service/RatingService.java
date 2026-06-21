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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

        // 8. Return rating for rating response of document
        return ratingMapper.toRatingResponse(rating, document);
    }

    // --------------------------------------------------------------------------------------------



    private User getCurrentUser() {
        return AiStudyHub.BE.security.SecurityUtils.getCurrentUser();
    }
}
