package AiStudyHub.BE.service;

import AiStudyHub.BE.constraint.ErrorCode;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.exception.GlobalException;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IToken;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class TokenService implements IToken {

    @Value("${jwt.signerKey}")
    @NonFinal
    private String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    @NonFinal
    private int VALID_DURATION;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    UserRepo userRepo;

    @Override
    public String generateAccessToken(User user) {
        byte[] keyBytes = Base64.getDecoder().decode(SIGNER_KEY);

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(Long.toString(user.getUserId()))
                .issuer("aistudyhub")
                .audience("aistudyhub-app")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope","ROLE_" + user.getRole().name())
                .build();
        JWSObject jwsObject = new JWSObject(header, new Payload(claims.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(keyBytes));
            return jwsObject.serialize();
        } catch (Exception e) {
            log.error("Token generation failed: {}", e.getMessage());
            throw new GlobalException(500, "Token generation failed");
        }
    }

    @Override
    public User verifyAccessToken(String token) {
        try {
            // 1. Decode Token
            Jwt jwt = jwtDecoder.decode(token);
            
            String subject = jwt.getSubject();
            if (subject == null) {
                throw new GlobalException(ErrorCode.INVALID_TOKEN_SUBJECT);
            }
            Long userID = Long.parseLong(subject);
            return userRepo.findById(userID)
                    .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN);
        }
    }
}
