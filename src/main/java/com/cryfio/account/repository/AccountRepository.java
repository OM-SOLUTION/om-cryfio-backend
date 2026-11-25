package com.cryfio.account.repository;

import com.cryfio.account.domain.Account;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT  account FROM Account account
        where account.id = :accountId
    """)
    Optional<Account> findByIdForUpdate(@Param("accountId") UUID accountId);
}
