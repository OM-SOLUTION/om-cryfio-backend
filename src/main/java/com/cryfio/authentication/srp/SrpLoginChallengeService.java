package com.cryfio.authentication.srp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class SrpLoginChallengeService {
    private static final String KEY_PREFIX = "srp:login-challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final int CHALLENGE_ID_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper ;
    public String create(
            UUID accountId,
            byte[] clientPublicValue,
            byte[] serverPrivateValue,
            byte[] serverPublicValue
    ){
        byte[] randomBytes = new byte[CHALLENGE_ID_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        String challengeId = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Challenge challenge = new Challenge(
                accountId,
                encode(clientPublicValue),
                encode(serverPrivateValue),
                encode(serverPublicValue)
        );
        stringRedisTemplate.opsForValue().set(
                redisKey(challengeId),serialize(challenge),CHALLENGE_TTL
        );
        return challengeId;
    }
    public Optional<Challenge> consume(String challengeId){
        if(challengeId == null || challengeId.isBlank()){
            return Optional.empty();
        }
        String storedValue = stringRedisTemplate.opsForValue().getAndDelete(redisKey(challengeId));
        if(storedValue == null){
            return Optional.empty();
        }
        return Optional.of(deserialize(storedValue));
    }
    private String serialize(Challenge challenge) {
        try {
            return objectMapper.writeValueAsString(challenge);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize SRP login challenge", exception);
        }
    }
    private Challenge deserialize(String storedValue){
        try {
            return objectMapper.readValue(storedValue, Challenge.class);
        }catch (JsonProcessingException exception){
            throw new IllegalStateException("Could not deserialize SRP login challenge",exception);
        }
    }
    private static String encode(byte[] value){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
    private static String redisKey(String challengeId){
        return KEY_PREFIX + sha256(challengeId);
    }
    private static String sha256(String value){
        try{
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }catch(NoSuchAlgorithmException exception){
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", exception);
        }
    }
    public record  Challenge(
            UUID accountId,
            String clientPublicValue,
            String serverPrivateValue,
            String serverPublicValue
    ){}
}
