package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.UpdateProfileResponse;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.mapper.UserMapper;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserUpdateService implements IUser {

    @Autowired
    UserRepo userRepo;
    @Autowired
    UserMapper userMapper;

    @Override
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromRequest(request, user);

        userRepo.save(user);


        return userMapper.toUpdateProfileResponse(user);
    }
}
