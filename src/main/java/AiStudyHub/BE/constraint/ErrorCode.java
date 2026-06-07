package AiStudyHub.BE.constraint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public enum ErrorCode {
    // Validation
    FIELD_REQUIRED("Cannot be blank", HttpStatus.BAD_REQUEST),
    INVALID_SIZE("must be between 5 and 50", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT("invalid format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(
            "Password must be at least 8 characters and include uppercase, lowercase, number and special character",
            HttpStatus.BAD_REQUEST
    ),
    PASSWORD_NOT_MATCH(
            "Confirm password does not match password",
            HttpStatus.BAD_REQUEST
    ),

    // Token
    INVALID_TOKEN("Invalid token", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_SUBJECT("Invalid token subject", HttpStatus.UNAUTHORIZED),

    // User
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    NOT_FOUND("not found", HttpStatus.NOT_FOUND),
    AVATAR_URL_TOO_LONG("Avatar URL must be less than 2000 characters", HttpStatus.BAD_REQUEST),

    // Authentication
    EMAIL_ALREADY_EXISTS("Email already exists", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("Please verify your email before login", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_VERIFIED("Email already verified", HttpStatus.BAD_REQUEST),
    ACCOUNT_BANNED("Account is banned", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("Invalid username or password", HttpStatus.UNAUTHORIZED),

    // OTP
    INVALID_OTP("Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("OTP expired", HttpStatus.BAD_REQUEST),;


    // DRY: Don't Repeat Yourself: cấm viết dòng code lặp đi lặp lại nhiều lần
    final String message;
    final HttpStatus httpStatus;

    public int getCode(){
        return httpStatus.value();
    }
}
