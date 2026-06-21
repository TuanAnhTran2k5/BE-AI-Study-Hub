package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.User;

public interface IStorageService {

    boolean validateStorage(User user, long fileSize);

    boolean increaseStorage(User user, long fileSize);

    boolean decreaseStorage(User user, long fileSize);
}
