package AiStudyHub.BE.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpUtil {

    SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a cryptographically secure 6-digit OTP code.
     * @return 6-digit string between "100000" and "999999"
     */
    public String generateSixDigitOtp() {
        return String.valueOf(secureRandom.nextInt(900000) + 100000);
    }

    /**
     * Check if a token string has the structure of a JWT (e.g. ID token vs opaque access token).
     * @param token token string to inspect
     * @return true if token starts with 'ey' and has 3 dot-separated segments
     */
    public boolean isJwtToken(String token) {
        return token != null && token.startsWith("ey") && token.split("\\.").length == 3;
    }
}
