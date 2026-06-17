package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.dto.Request.RatingRequest;
import AiStudyHub.BE.dto.Response.RatingResponse;

public interface IRating {
    RatingResponse submitRating(Long documentId, RatingRequest request);
}
