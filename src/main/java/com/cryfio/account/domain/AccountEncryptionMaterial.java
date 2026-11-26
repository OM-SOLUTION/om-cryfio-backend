package com.cryfio.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "account_encryption_material")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEncryptionMaterial {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "schema_version", nullable = false)
    private short schemaVersion;

    @Column(name = "identity_algorithm", nullable = false, length = 32)
    private String identityAlgorithm;

    @Column(name = "identity_public_key", nullable = false)
    private byte[] identityPublicKey;

    @Column(name = "backup_version", nullable = false)
    private short backupVersion;

    @Column(name = "backup_algorithm", nullable = false, length = 128)
    private String backupAlgorithm;

    @Column(name = "backup_public_key", nullable = false)
    private byte[] backupPublicKey;

    @Column(name = "kdf_algorithm", nullable = false, length = 32)
    private String kdfAlgorithm;

    @Column(name = "kdf_salt", nullable = false)
    private byte[] kdfSalt;

    @Column(name = "kdf_memory_kib", nullable = false)
    private int kdfMemoryKiB;

    @Column(name = "kdf_iterations", nullable = false)
    private int kdfIterations;

    @Column(name = "kdf_parallelism", nullable = false)
    private int kdfParallelism;

    @Column(name = "kdf_output_bytes", nullable = false)
    private int kdfOutputBytes;

    @Column(name = "cipher_algorithm", nullable = false, length = 32)
    private String cipherAlgorithm;

    @Column(name = "cipher_nonce", nullable = false)
    private byte[] cipherNonce;

    @Column(name = "cipher_ciphertext", nullable = false)
    private byte[] cipherCiphertext;

    @Column(name = "cipher_tag", nullable = false)
    private byte[] cipherTag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private AccountEncryptionMaterial(
            Account account,
            short schemaVersion,
            String identityAlgorithm,
            byte[] identityPublicKey,
            short backupVersion,
            String backupAlgorithm,
            byte[] backupPublicKey,
            String kdfAlgorithm,
            byte[] kdfSalt,
            int kdfMemoryKiB,
            int kdfIterations,
            int kdfParallelism,
            int kdfOutputBytes,
            String cipherAlgorithm,
            byte[] cipherNonce,
            byte[] cipherCiphertext,
            byte[] cipherTag
    ) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.schemaVersion = schemaVersion;
        this.identityAlgorithm = Objects.requireNonNull(identityAlgorithm, "Identity algorithm is required");
        this.identityPublicKey = Objects.requireNonNull(identityPublicKey,"Identity public key is required");
        this.backupVersion = backupVersion;
        this.backupAlgorithm = Objects.requireNonNull(backupAlgorithm,"Backup algorithm is required");
        this.backupPublicKey = Objects.requireNonNull(backupPublicKey,"Backup public key is required");
        this.kdfAlgorithm = Objects.requireNonNull(kdfAlgorithm,"KDF algorithm is required");
        this.kdfSalt = Objects.requireNonNull(kdfSalt, "KDF salt is required");
        this.kdfMemoryKiB = kdfMemoryKiB;
        this.kdfIterations = kdfIterations;
        this.kdfParallelism = kdfParallelism;
        this.kdfOutputBytes = kdfOutputBytes;
        this.cipherAlgorithm = Objects.requireNonNull(cipherAlgorithm, "Cipher algorithm is required");
        this.cipherNonce = Objects.requireNonNull(cipherNonce, "Cipher nonce is required");
        this.cipherCiphertext = Objects.requireNonNull(cipherCiphertext, "Ciphertext is required");
        this.cipherTag = Objects.requireNonNull(cipherTag, "Cipher tag is required");
    }

    public static AccountEncryptionMaterial create(
            Account account,
            short schemaVersion,
            String identityAlgorithm,
            byte[] identityPublicKey,
            short backupVersion,
            String backupAlgorithm,
            byte[] backupPublicKey,
            String kdfAlgorithm,
            byte[] kdfSalt,
            int kdfMemoryKiB,
            int kdfIterations,
            int kdfParallelism,
            int kdfOutputBytes,
            String cipherAlgorithm,
            byte[] cipherNonce,
            byte[] cipherCiphertext,
            byte[] cipherTag
    ) {
        return new AccountEncryptionMaterial(
                account,
                schemaVersion,
                identityAlgorithm,
                identityPublicKey,
                backupVersion,
                backupAlgorithm,
                backupPublicKey,
                kdfAlgorithm,
                kdfSalt,
                kdfMemoryKiB,
                kdfIterations,
                kdfParallelism,
                kdfOutputBytes,
                cipherAlgorithm,
                cipherNonce,
                cipherCiphertext,
                cipherTag
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