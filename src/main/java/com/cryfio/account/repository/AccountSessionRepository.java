package com.cryfio.account.repository;

import com.cryfio.account.domain.AccountSession;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountSessionRepository extends JpaRepository<AccountSession, UUID> {
    Optional<AccountSession> findByRefreshTokenHash(byte[] refreshTokenHash);
    List<AccountSession> findAllByAccount_IdAndRevokedAtIsNullOrderByLastActivityAtDesc(UUID accountId);
    List<AccountSession> findAllByAccount_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActivityAtDesc(UUID accountId, Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT session FROM AccountSession  session
        JOIN FETCH session.account
        WHERE session.refreshTokenHash = :refreshTokenHash
    """)
    Optional<AccountSession> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash")byte[] refreshTokenHash);
    Optional<AccountSession> findByIdAndAccount_Id(UUID accountId, UUID id);
}
