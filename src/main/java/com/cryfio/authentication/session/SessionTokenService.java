package com.cryfio.authentication.session;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionTokenService {
    public  static final Duration ACCES_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(6);
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    public String createAccessToken(UUID accountId, UUID sessionId, Instant issuedAt) {
        Instant expiresAt  = issuedAt.plus(ACCES_TOKEN_TTL);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("sid",sessionId.toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
    }
    public RefreshToken createRefreshToken(Instant issuedAt) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return new RefreshToken(rawToken,sha256(rawToken),issuedAt.plus(REFRESH_TOKEN_TTL));
    }

    public byte[] hashRefreshToken(String refreshToken) {
        return sha256(refreshToken);
    }
    private static byte[] sha256(String value) {

        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException( "SHA-256 must be available in the Java runtime",exception);
        }
    }
    public record RefreshToken(
            String value,
            byte[] hash,
            Instant expiresAt
    ){}

}
