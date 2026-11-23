package com.cryfio.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "account_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "refresh_token_hash", nullable = false)
    private byte[] refreshTokenHash;

    @Column(name = "device_name", nullable = false, length = 128)
    private String deviceName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    private AccountSession(
            Account account,
            byte[] refreshTokenHash,
            String deviceName,
            Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.account = Objects.requireNonNull(account, "Account is required");
        this.refreshTokenHash = requireRefreshTokenHash(refreshTokenHash);
        this.deviceName = normalizeDeviceName(deviceName);
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiry time is required");
    }

    public static AccountSession create(
            Account account,
            byte[] refreshTokenHash,
            String deviceName,
            Instant expiresAt
    ) {
        return new AccountSession(
                account,
                refreshTokenHash,
                deviceName,
                expiresAt
        );
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(revokedAt, "Revocation time is required");
        }
    }

    public void updateLastActivity(Instant lastActivityAt) {
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "Last activity time is required");
    }
    public boolean isUsable(Instant now){
        return this.revokedAt == null && this.expiresAt.isAfter(now);
    }
    public void rotateRefreshToken(byte[] refreshTokenHash) {
        this.refreshTokenHash = requireRefreshTokenHash(refreshTokenHash);
    }
    private static byte[] requireRefreshTokenHash(byte[] refreshTokenHash) {
        if (refreshTokenHash == null || refreshTokenHash.length != 32) {
            throw new IllegalArgumentException("Refresh token hash must contain exactly 32 bytes");
        }

        return refreshTokenHash;
    }

    private static String normalizeDeviceName(String deviceName) {
        if (deviceName == null) {
            throw new IllegalArgumentException("Device name is required");
        }

        String normalized = deviceName.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be blank");
        }

        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Device name cannot exceed 128 characters");
        }

        return normalized;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastActivityAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.lastActivityAt = Instant.now();
    }
}