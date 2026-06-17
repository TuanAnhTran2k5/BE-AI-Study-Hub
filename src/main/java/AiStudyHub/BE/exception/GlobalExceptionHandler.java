package AiStudyHub.BE.exception;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
@Slf4j // enables SLF4J logging
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<APIResponse<Object>> handleGlobal(GlobalException exception) {
        int status = (exception.getCode() != null)
                ? exception.getCode() // HTTP status code value
                : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(status).body(APIResponse.response(status, exception.getMessage(), null));
    }

    // Catch-all: any unhandled exception returns a generic 500 instead of leaking
    // a stack trace / Spring's default error page to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.response(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal server error",
                        null
                ));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    // Catches validation exceptions from the Bean Validation library and returns
    // errors to the client (handles both @RequestBody and @ModelAttribute validations)
    public ResponseEntity<APIResponse<Object>> handleValidation(org.springframework.validation.BindException exception) {
        List<String> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String enumKey = error.getDefaultMessage();
                    ErrorCode errorCode;
                    try {
                        errorCode = ErrorCode.valueOf(enumKey);
                    } catch (IllegalArgumentException e) {
                        errorCode = ErrorCode.FIELD_REQUIRED;
                    }
                    return error.getField() + " " + errorCode.getMessage();
                }).toList();
        return ResponseEntity.badRequest().body(APIResponse.response(400, "validation error", errors));
    }

    // Handles malformed request bodies (e.g. invalid enum values or bad JSON) that
    // Jackson cannot deserialize, returning a 400 instead of the catch-all 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> handleNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(APIResponse.response(400, "Invalid request body", null));
    }
}
