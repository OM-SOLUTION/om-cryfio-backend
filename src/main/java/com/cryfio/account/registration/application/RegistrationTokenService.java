package com.cryfio.account.registration.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegistrationTokenService {
    private static final String KEY_PREFIX = "registration:token:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;

    public String issue(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");

        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        stringRedisTemplate.opsForValue().set(
                redisKey(rawToken),
                accountId.toString(),
                TOKEN_TTL
        );
        return rawToken;
    }

    public Optional<UUID> findAccountId(String rawToken) {
        if(rawToken == null || rawToken.isBlank()) return Optional.empty();

        String accountId = stringRedisTemplate.opsForValue()
                .get(redisKey(rawToken));

        if(accountId == null){
            return Optional.empty();
        }

        try{
            return Optional.of(UUID.fromString(accountId));
        }catch(IllegalArgumentException exception){
            throw new IllegalStateException("Redis contains an invalid registration token value",exception);
        }
    }
    public void revoke(String rawToken){
        if(rawToken != null && !rawToken.isBlank()){
            stringRedisTemplate.delete(redisKey(rawToken));
        }
    }
    public static String redisKey(String rawToken){
        return KEY_PREFIX + sha256(rawToken);
    }
    private static String sha256(String value){
        try{
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime",exception);
        }
    }
}
