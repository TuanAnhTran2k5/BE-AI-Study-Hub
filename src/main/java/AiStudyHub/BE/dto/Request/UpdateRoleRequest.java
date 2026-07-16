package AiStudyHub.BE.dto.Request;

import AiStudyHub.BE.constraint.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoleRequest {
    @NotNull(message = "Role is required")
    UserRole role;
}
