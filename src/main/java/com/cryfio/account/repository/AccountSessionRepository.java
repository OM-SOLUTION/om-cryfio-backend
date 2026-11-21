package com.cryfio.account.repository;

import com.cryfio.account.domain.AccountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountSessionRepository extends JpaRepository<AccountSession, UUID> {
    Optional<AccountSession> findByRefreshTokenHash(byte[] refreshTokenHash);
    List<AccountSession> findAllByAccount_IdAndRevokedAtIsNullOrderByLastActivityAtDesc(UUID accountId);
}
