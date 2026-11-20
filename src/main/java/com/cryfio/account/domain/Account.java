package com.cryfio.account.domain;

import com.cryfio.account.domain.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    public static final long DEFAULT_QUOTA_BYTES =  5L*1024L*1024L*1024L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "quota_bytes", nullable = false)
    private long quotaBytes;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "purge_scheduled_at")
    private Instant purgeScheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    private Account(String email) {
        this.email = normalizeEmail(email);
        this.status = AccountStatus.PENDING_REGISTRATION;
        this.quotaBytes = DEFAULT_QUOTA_BYTES;
    }

    public static Account createPending(String email) {
        return new Account(email);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        if (normalized.length() > 320) {
            throw new IllegalArgumentException(
                    "Email cannot exceed 320 characters"
            );
        }

        return normalized;
    }

    public void markEmailVerified(Instant verifiedAt) {
        if(this.emailVerifiedAt != null) {
            throw new IllegalArgumentException("Email already verified");
        }
        this.emailVerifiedAt = Objects.requireNonNull(verifiedAt,"Verification time required");
    }
    public void activate(Instant activatedAt){
        if(this.emailVerifiedAt == null) {
            throw new IllegalArgumentException("Email must be verified before account activation");
        }
        if(this.status != AccountStatus.PENDING_REGISTRATION){
            throw new IllegalStateException("Only a pending registration can be activated");
        }
        this.status = AccountStatus.ACTIVE;
        this.activatedAt = Objects.requireNonNull(activatedAt,"Activation time required");
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
