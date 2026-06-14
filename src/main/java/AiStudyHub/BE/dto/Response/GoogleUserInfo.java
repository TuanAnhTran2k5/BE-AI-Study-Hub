package AiStudyHub.BE.dto.Response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty("email_verified")
    String emailVerified;
    String name;
    String picture;
    @JsonProperty("given_name")
    String givenName;
    @JsonProperty("family_name")
    String familyName;
    String locale;
}
