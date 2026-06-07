package AiStudyHub.BE.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JWTConfig {
    @Value("${jwt.signerKey}")
    private String jwtSingerKey;


    //2. Check chữ ký
    //3. check hạn sử dụng
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] decoderSecret = Base64.getDecoder().decode(jwtSingerKey);
        SecretKey key = new SecretKeySpec(decoderSecret, "HmacSHA512");

        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}
