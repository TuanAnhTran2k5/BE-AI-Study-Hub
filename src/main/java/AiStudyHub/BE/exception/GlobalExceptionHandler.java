package AiStudyHub.BE.exception;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.dto.Response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        return ResponseEntity.status(status).body(APIResponse.response(status, exception.getMessage(),null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Catches validation exceptions from the Bean Validation library and returns errors to the client
    public ResponseEntity<APIResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->{
                    String enumKey = error.getDefaultMessage();
                    ErrorCode errorCode;
                    try {
                        errorCode = ErrorCode.valueOf(enumKey);
                    }catch (IllegalArgumentException e) {
                        errorCode = ErrorCode.FIELD_REQUIRED;
                    }
                    return error.getField() + " " + errorCode.getMessage();
                }).toList();
        return ResponseEntity.badRequest().body(APIResponse.response(400,"validation error",errors));
    }
}
