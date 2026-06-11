package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.service.impl.IStorageService;
import org.springframework.stereotype.Service;

@Service
public class StorageService implements IStorageService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB
    private static final long DEFAULT_STORAGE_LIMIT = 2L * 1024 * 1024 * 1024; // 2GB

    @Override
    public void validateUpload(User user, long fileSize){

        if (fileSize > MAX_FILE_SIZE) {
            throw new GlobalException(ErrorCode.FILE_TOO_LARGE);
        }

        long storageUsed = user.getStorageUsed() == null
                ? 0L
                : user.getStorageUsed();

        long storageLimit = user.getStorageLimit() == null
                ? DEFAULT_STORAGE_LIMIT
                : user.getStorageLimit();

        if (storageUsed + fileSize > storageLimit) {
            throw new GlobalException(ErrorCode.STORAGE_LIMIT_EXCEEDED);
        }
    }

    @Override
    public void increaseStorage(User user, long fileSize) {
        long storageUsed = user.getStorageUsed() == null
                ? 0L
                : user.getStorageUsed();

        user.setStorageUsed(storageUsed + fileSize);
    }

    @Override
    public void decreaseStorage(User user, long fileSize) {
        long storageUsed = user.getStorageUsed() == null
                ? 0L
                : user.getStorageUsed();

        long newStorageUsed = Math.max(0L, storageUsed - fileSize);

        user.setStorageUsed(newStorageUsed);
    }
}