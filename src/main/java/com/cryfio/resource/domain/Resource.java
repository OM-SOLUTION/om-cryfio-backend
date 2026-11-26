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
@Table(name = "resources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 16)
    private ResourceType resourceType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "file_extension", length = 255)
    private String fileExtension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Resource parentFolder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_account_id", nullable = false)
    private Account owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    private Account createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modified_by_account_id", nullable = false)
    private Account modifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    private Resource(
            ResourceType resourceType,
            Account owner,
            Account createdBy,
            Resource parentFolder,
            String name,
            String fileExtension
    ) {
        this.resourceType = resourceType;
        this.owner = Objects.requireNonNull(owner, "Owner is required");
        this.createdBy = Objects.requireNonNull(createdBy, "Owner is required");
        this.modifiedBy  = createdBy;
        this.parentFolder = parentFolder;
        this.name = normalizeName(name);
        this.fileExtension = fileExtension;
    }

    public static Resource createFolder(Account owner, Resource parentFolder, String name) {
        return new Resource(ResourceType.FOLDER, owner, owner, parentFolder, name,null);
    }
    public static Resource createFile(Account owner, Account createdBy,Resource parentFolder, String name, String fileExtension) {
        return new Resource(ResourceType.FILE, owner, createdBy, parentFolder, name, normalizeFileExtension(fileExtension));
    }

    private static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Folder name is required");
        }

        String normalized = name.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be blank");
        }

        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Folder name cannot exceed 255 characters");
        }

        return normalized;
    }
    private static String normalizeFileExtension(String fileExtension) {
        if(fileExtension == null || fileExtension.isBlank()) {
            return null;
        }
        String normalized =  fileExtension.trim().toLowerCase();
        if(normalized.isEmpty() || normalized.length() > 255) {
            throw new IllegalArgumentException("Folder file extension cannot be blank");
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
