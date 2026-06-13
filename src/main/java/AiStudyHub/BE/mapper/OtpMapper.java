package AiStudyHub.BE.mapper;

import AiStudyHub.BE.dto.Response.ForgotPasswordResponse;
import AiStudyHub.BE.dto.Response.ResendOtpResponse;
import AiStudyHub.BE.entity.OtpVerification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OtpMapper {
    ForgotPasswordResponse toForgotPasswordResponse(OtpVerification otp);

    ResendOtpResponse toResendOtpResponse(OtpVerification otp);
}
