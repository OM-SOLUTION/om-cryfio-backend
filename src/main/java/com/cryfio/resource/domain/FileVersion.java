package com.cryfio.resource.domain;

import com.cryfio.account.domain.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "file_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileVersionStatus status;

    @Column(name = "storage_object_key", nullable = false, length = 255)
    private String storageObjectKey;

    @Column(name = "multipart_upload_id", length = 512)
    private String multipartUploadId;

    @Column(name = "upload_url_expires_at", nullable = false)
    private Instant uploadUrlExpiresAt;

    @Column(name = "plaintext_size_bytes", nullable = false)
    private long plaintextSizeBytes;

    @Column(name = "encrypted_size_bytes", nullable = false)
    private long encryptedSizeBytes;

    @Column(name = "ciphertext_sha256", nullable = false)
    private byte[] ciphertextSha256;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_account_id", nullable = false)
    private Account uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    private FileVersion(
            Resource resource,
            int versionNumber,
            String storageObjectKey,
            Instant uploadUrlExpiresAt,
            long plaintextSizeBytes,
            long encryptedSizeBytes,
            byte[] ciphertextSha256,
            int chunkCount,
            Account uploadedBy
    ) {
        if (resource.getResourceType() != ResourceType.FILE) {
            throw new IllegalArgumentException("A version can only belong to a file");
        }

        if (versionNumber < 1) {
            throw new IllegalArgumentException("Version number must be at least 1");
        }

        if (storageObjectKey == null ||
            storageObjectKey.isBlank() ||
            storageObjectKey.length() > 255
        ) {
            throw new IllegalArgumentException("Storage object key is invalid");
        }

        if (plaintextSizeBytes < 0) {
            throw new IllegalArgumentException("Plaintext size cannot be negative");
        }

        if (encryptedSizeBytes <= 0) {
            throw new IllegalArgumentException("Encrypted size must be greater than zero");
        }

        if (ciphertextSha256 == null
                || ciphertextSha256.length != 32) {
            throw new IllegalArgumentException("Ciphertext SHA-256 must contain exactly 32 bytes");
        }

        if (chunkCount < 1) {
            throw new IllegalArgumentException("Chunk count must be at least 1");
        }

        this.resource = Objects.requireNonNull(resource, "Resource is required");
        this.versionNumber = versionNumber;
        this.status = FileVersionStatus.PENDING_UPLOAD;
        this.storageObjectKey = storageObjectKey;
        this.uploadUrlExpiresAt = Objects.requireNonNull(uploadUrlExpiresAt, "Upload URL expiry is required");
        this.plaintextSizeBytes = plaintextSizeBytes;
        this.encryptedSizeBytes = encryptedSizeBytes;
        this.ciphertextSha256 = ciphertextSha256;
        this.chunkCount = chunkCount;
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "Uploader is required");
    }

    public static FileVersion createPending(
            Resource resource,
            int versionNumber,
            String storageObjectKey,
            Instant uploadUrlExpiresAt,
            long plaintextSizeBytes,
            long encryptedSizeBytes,
            byte[] ciphertextSha256,
            int chunkCount,
            Account uploadedBy
    ) {
        return new FileVersion(
                resource,
                versionNumber,
                storageObjectKey,
                uploadUrlExpiresAt,
                plaintextSizeBytes,
                encryptedSizeBytes,
                ciphertextSha256,
                chunkCount,
                uploadedBy
        );
    }

    public void setMultipartUploadId(String multipartUploadId) {
        if (multipartUploadId == null || multipartUploadId.isBlank()) {
            throw new IllegalArgumentException("Multipart upload ID is required");
        }

        if (this.multipartUploadId != null) {
            throw new IllegalStateException("Multipart upload ID is already set");
        }

        this.multipartUploadId = multipartUploadId;
    }

    public void activate(Instant activatedAt) {
        if (this.status != FileVersionStatus.PENDING_UPLOAD) {
            throw new IllegalStateException("Only a pending upload can be activated");
        }

        this.status = FileVersionStatus.ACTIVE;
        this.activatedAt = Objects.requireNonNull(activatedAt, "Activation time is required");
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }
}