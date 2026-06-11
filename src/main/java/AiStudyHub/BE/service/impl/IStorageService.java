package AiStudyHub.BE.service.impl;

import AiStudyHub.BE.entity.User;

public interface IStorageService {

    void validateStorage(User user, long fileSize);

    void increaseStorage(User user, long fileSize);

    void decreaseStorage(User user, long fileSize);
}