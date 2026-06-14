package AiStudyHub.BE.dto.Response;

import AiStudyHub.BE.constraint.OtpPurpose;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResendOtpResponse {
    String email;
    OtpPurpose purpose;
    LocalDateTime otpExpiredAt;
}
