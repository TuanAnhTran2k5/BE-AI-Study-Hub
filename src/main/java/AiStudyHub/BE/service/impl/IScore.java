package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.User;


public interface IScore {

    record ScoreTypeSpec(String code, String name, int defaultPoint, String description) {
    }

    int awardScore(User user, Document document, ScoreTypeSpec spec, int scoreChange, String description);
}
