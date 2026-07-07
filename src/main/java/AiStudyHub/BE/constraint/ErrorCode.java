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
    UNAUTHENTICATED("Unauthenticated", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Invalid token", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_SUBJECT("Invalid token subject", HttpStatus.UNAUTHORIZED),

    // User
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    NOT_FOUND("not found", HttpStatus.NOT_FOUND),
    AVATAR_URL_TOO_LONG("Avatar URL must be less than 2000 characters", HttpStatus.BAD_REQUEST),

    // Document
    SUBJECT_NOT_FOUND("Subject not found", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("File upload to storage failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED("File delete from storage failed", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_NOT_FOUND("Document not found", HttpStatus.NOT_FOUND),
    DOCUMENT_DELETE_FAILED("Delete document failed",  HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DOWNLOAD_FAILED("Fail to download file", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_NOT_PUBLIC("Document is not public", HttpStatus.FORBIDDEN),
    FORBIDDEN_DOWNLOAD_CLOUD_DOCUMENT( "You can only download documents from your own cloud storage", HttpStatus.FORBIDDEN),
    FORBIDDEN_UPDATE_DOCUMENT("You can only update your own documents", HttpStatus.FORBIDDEN),
    CANNOT_EDIT_DOWNLOADED_DOCUMENT("Downloaded documents cannot be edited", HttpStatus.FORBIDDEN),

    // Authentication
    EMAIL_ALREADY_EXISTS("Email already exists", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("Please verify your email before login", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_VERIFIED("Email already verified", HttpStatus.BAD_REQUEST),
    ACCOUNT_BANNED("Account is banned", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("Invalid username or password", HttpStatus.UNAUTHORIZED),

    // OTP
    INVALID_OTP("Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("OTP expired", HttpStatus.BAD_REQUEST),

    // File storage
    FILE_TOO_LARGE("File exceeds 20MB", HttpStatus.BAD_REQUEST),
    STORAGE_LIMIT_EXCEEDED("2GB storage capacity exceeded", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_SIZE("Image exceeds 5MB", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE("Only image files (JPG, PNG, WEBP, GIF, AVIF, etc.) are supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_IMAGE_FILE("Corrupted or invalid image file", HttpStatus.BAD_REQUEST),

    // Rating
    INVALID_RATING_VALUE("Rating value must be an integer between 1 and 5", HttpStatus.BAD_REQUEST),
    CANNOT_RATE_OWN_DOCUMENT("You cannot rate your own document", HttpStatus.FORBIDDEN),

    // Badge
    BADGE_NOT_FOUND("Badge not found", HttpStatus.NOT_FOUND),
    BADGE_ALREADY_EXISTS("Badge name already exists", HttpStatus.BAD_REQUEST),
    ;



    // DRY: Don't Repeat Yourself — avoid writing the same code more than once
    final String message;
    final HttpStatus httpStatus;



    public int getCode(){
        return httpStatus.value();
    }
}
