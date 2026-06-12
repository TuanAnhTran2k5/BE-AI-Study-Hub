package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Request.RegisterRequest;
import AiStudyHub.BE.dto.Request.UpdateProfileRequest;
import AiStudyHub.BE.dto.Response.UpdateProfileResponse;
import AiStudyHub.BE.dto.Response.UserResponse;
import AiStudyHub.BE.entity.User;

import org.mapstruct.*;

@Mapper(componentModel = "spring") // marks this interface as a MapStruct mapper
public interface UserMapper {

    UserResponse toUserResponse(User user);

    UpdateProfileResponse toUpdateProfileResponse(User user);

    @Mapping(target = "passwordHash", source = "password")
    User toUser(RegisterRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User updateUserFromRequest(UpdateProfileRequest request, @MappingTarget User user);
}
