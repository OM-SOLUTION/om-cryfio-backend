package com.cryfio.account.repository;

import com.cryfio.account.domain.AccountEncryptionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountEncryptionMaterialRepository extends JpaRepository<AccountEncryptionMaterial, UUID> {
}
