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
@Table(name = "account_email_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEmailVerification {
    public static final short MAX_FAILED_ATTEMPTS = 5;

    @Id
    @Column(name = "account_id",nullable = false,updatable = false)
    private UUID accountId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempt_count", nullable = false)
    private short failedAttemptCount;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private AccountEmailVerification(Account account, String codeHash, Instant expiresAt, Instant resendAvailableAt) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.codeHash = Objects.requireNonNull(codeHash,"Code hash is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiry time is required");
        this.resendAvailableAt = Objects.requireNonNull(resendAvailableAt,"Resend availability time is required");
    }

    public static AccountEmailVerification create(Account account, String codeHash, Instant expiresAt, Instant resendAvailableAt) {
        return new AccountEmailVerification(
                account,
                codeHash,
                expiresAt,
                resendAvailableAt
        );
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean hasTooManyFailedAttempts() {
        return failedAttemptCount >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailedAttempt() {
        if (!hasTooManyFailedAttempts()) {
            failedAttemptCount++;
        }
    }

    public void markVerified(Instant verifiedAt) {
        if (this.verifiedAt != null) {
            throw new IllegalStateException("Email is already verified");
        }

        this.verifiedAt = Objects.requireNonNull(verifiedAt,"Verification time is required");
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
