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
@Slf4j // ghi log dưới terminal
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<APIResponse<Object>> handleGlobal(GlobalException exception) {
        int status = (exception.getCode() != null)
                ? exception.getCode() // value của code
                : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(status).body(APIResponse.response(status, exception.getMessage(),null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // hàm bắt cấu hình exception của thư viện validation rồi trả về FE
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
