package com.cryfio.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
@Getter
@Entity
@Table(name = "account_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountProfile {

    private static final Pattern USERNAME_PATTERN =  Pattern.compile("^[a-z0-9_]{3,30}$");

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "username", length = 30)
    private String username;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;



    private AccountProfile(Account account,String firstName,String lastName,String username) {
        this.account = Objects.requireNonNull(account, "Account is required");
        this.firstName = normalizeName(firstName, "First name");
        this.lastName = normalizeName(lastName, "Last name");
        this.username = normalizeUsername(username);
    }

    public static AccountProfile create(Account account, String firstName, String lastName, String username) {
        return new AccountProfile(
                account,
                firstName,
                lastName,
                username
        );
    }

    public void update(String firstName, String lastName, String username) {
        this.firstName = normalizeName(firstName, "First name");
        this.lastName = normalizeName(lastName, "Last name");
        this.username = normalizeUsername(username);
    }

    private static String normalizeName(
            String value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 100 characters");
        }

        return normalized;
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String normalized = username.trim().toLowerCase(Locale.ROOT);

        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Username must contain 3 to 30 lowercase letters, numbers, or underscores");
        }

        return normalized;
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