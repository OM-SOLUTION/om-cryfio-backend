package com.cryfio.authentication.session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRegistryService {
    private static final String KEY_PREFIX = "auth:session:";
    private final StringRedisTemplate stringRedisTemplate;

    public void activate(UUID sessionId, UUID accountId, Instant expiresAt){
        Duration ttl = Duration.between(Instant.now(),expiresAt);
        if(ttl.isZero() || ttl.isNegative()){
            throw new IllegalStateException("Session expiry must be in the future");
        }
        stringRedisTemplate.opsForValue().set(redisKey(sessionId), accountId.toString(),ttl);
    }
    public boolean isActive(UUID sessionId, UUID accountId){
        String storedAccountId = stringRedisTemplate.opsForValue().get(redisKey(sessionId));
        return accountId.toString().equals(storedAccountId);
    }
    public Optional<UUID> findAccountId(UUID sessionId){
        String storedAccountId = stringRedisTemplate.opsForValue().get(redisKey(sessionId));
        if(storedAccountId == null){
            return Optional.empty();
        }
        try{
            return Optional.of(UUID.fromString(storedAccountId));
        }catch (IllegalArgumentException exception){
            throw new IllegalStateException("Redis contains an invalid session account ID",exception);
        }
    }

    public void revoke(UUID sessionId){
        stringRedisTemplate.delete(redisKey(sessionId));
    }
    private static String redisKey(UUID sessionId){
        return KEY_PREFIX + sessionId;
    }
}
