package com.cryfio.account.repository;

import com.cryfio.account.domain.AccountSrpCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountSrpCredentialRepository extends JpaRepository<AccountSrpCredential, UUID> {
}
