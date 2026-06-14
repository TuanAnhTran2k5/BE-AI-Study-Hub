package AiStudyHub.BE.dto.Response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleUserInfo {
    String iss;
    String aud;
    String sub;
    String email;
    String email_verified;
    String name;
    String picture;
    String given_name;
    String family_name;
    String locale;
}
