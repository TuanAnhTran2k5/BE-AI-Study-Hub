package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.UpdateProfileResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") //đánh dấu đây là interface mapper
public interface UserMapper {
    // chuyển đổi từ Entity sang Response
    UserResponse toUserResponse(User user);

    UpdateProfileResponse toUpdateProfileResponse(User user);
}
