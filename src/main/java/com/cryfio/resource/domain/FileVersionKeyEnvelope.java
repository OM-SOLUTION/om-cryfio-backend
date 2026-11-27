package com.cryfio.resource.domain;

import com.cryfio.account.domain.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "file_version_key_envelopes")
@IdClass(FileVersionKeyEnvelopeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVersionKeyEnvelope {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_version_id", nullable = false)
    private FileVersion fileVersion;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_account_id", nullable = false)
    private Account recipientAccount;

    @Column(name = "encrypted_dek", nullable = false)
    private byte[] encryptedDek;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private FileVersionKeyEnvelope(FileVersion fileVersion, Account recipientAccount, byte[] encryptedDek) {
        if (encryptedDek == null || encryptedDek.length == 0) {
            throw new IllegalArgumentException("Encrypted DEK is required");
        }

        this.fileVersion = Objects.requireNonNull(fileVersion, "File version is required");
        this.recipientAccount = Objects.requireNonNull(recipientAccount, "Recipient account is required");
        this.encryptedDek = encryptedDek;
    }

    public static FileVersionKeyEnvelope create(FileVersion fileVersion, Account recipientAccount, byte[] encryptedDek) {
        return new FileVersionKeyEnvelope(
                fileVersion,
                recipientAccount,
                encryptedDek
        );
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }
}
