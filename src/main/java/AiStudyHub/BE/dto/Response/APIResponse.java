package AiStudyHub.BE.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {

    int code;
    String message;
    T result;

    public static <T> APIResponse<T> response(int code, String message, T result) {

        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .result(result)
                .build();
    }
}
