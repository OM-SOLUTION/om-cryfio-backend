package com.cryfio.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "account_srp_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountSrpCredential {
    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "srp_protocol", nullable = false, length = 16)
    private String srpProtocol;

    @Column(name = "srp_group_id", nullable = false, length = 32)
    private String srpGroupId;

    @Column(name = "srp_hash_algorithm", nullable = false, length = 32)
    private String srpHashAlgorithm;

    @Column(name = "srp_verifier", nullable = false)
    private byte[] srpVerifier;

    @Column(name = "password_kdf_algorithm", nullable = false, length = 32)
    private String passwordKdfAlgorithm;

    @Column(name = "password_kdf_salt", nullable = false)
    private byte[] passwordKdfSalt;

    @Column(name = "password_kdf_memory_kib", nullable = false)
    private int passwordKdfMemoryKiB;

    @Column(name = "password_kdf_iterations", nullable = false)
    private int passwordKdfIterations;

    @Column(name = "password_kdf_parallelism", nullable = false)
    private int passwordKdfParallelism;

    @Column(name = "password_kdf_output_bytes", nullable = false)
    private int passwordKdfOutputBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private AccountSrpCredential(
            Account account,
            String srpProtocol,
            String srpGroupId,
            String srpHashAlgorithm,
            byte[] srpVerifier,
            String passwordKdfAlgorithm,
            byte[] passwordKdfSalt,
            int passwordKdfMemoryKiB,
            int passwordKdfIterations,
            int passwordKdfParallelism,
            int passwordKdfOutputBytes
    ) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.srpProtocol = Objects.requireNonNull(srpProtocol, "SRP protocol is required");
        this.srpGroupId = Objects.requireNonNull(srpGroupId, "SRP group ID is required");
        this.srpHashAlgorithm = Objects.requireNonNull(srpHashAlgorithm, "SRP hash algorithm is required");
        this.srpVerifier = Objects.requireNonNull(srpVerifier, "SRP verifier is required");
        this.passwordKdfAlgorithm = Objects.requireNonNull(passwordKdfAlgorithm, "Password KDF algorithm is required");
        this.passwordKdfSalt = Objects.requireNonNull(passwordKdfSalt,"Password KDF salt is required");
        this.passwordKdfMemoryKiB = passwordKdfMemoryKiB;
        this.passwordKdfIterations = passwordKdfIterations;
        this.passwordKdfParallelism = passwordKdfParallelism;
        this.passwordKdfOutputBytes = passwordKdfOutputBytes;
    }

    public static AccountSrpCredential create(
            Account account,
            String srpProtocol,
            String srpGroupId,
            String srpHashAlgorithm,
            byte[] srpVerifier,
            String passwordKdfAlgorithm,
            byte[] passwordKdfSalt,
            int passwordKdfMemoryKiB,
            int passwordKdfIterations,
            int passwordKdfParallelism,
            int passwordKdfOutputBytes
    ) {
        return new AccountSrpCredential(
                account,
                srpProtocol,
                srpGroupId,
                srpHashAlgorithm,
                srpVerifier,
                passwordKdfAlgorithm,
                passwordKdfSalt,
                passwordKdfMemoryKiB,
                passwordKdfIterations,
                passwordKdfParallelism,
                passwordKdfOutputBytes
        );
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
