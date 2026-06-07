package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.User;

public interface IToken {
    String generateAccessToken(User user);

    User verifyAccessToken(String token);
}
