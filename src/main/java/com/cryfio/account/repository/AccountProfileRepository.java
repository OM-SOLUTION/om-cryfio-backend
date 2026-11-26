package com.cryfio.account.repository;

import com.cryfio.account.domain.AccountProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountProfileRepository extends JpaRepository<AccountProfile, UUID> {
    Optional<AccountProfile> findByUsername(String username);
    boolean existsByUsername(String username);
}
