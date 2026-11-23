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

    private Resource(Account owner, Resource parentFolder, String name) {
        this.resourceType = ResourceType.FOLDER;
        this.owner = Objects.requireNonNull(owner, "Owner is required");
        this.createdBy = owner;
        this.modifiedBy = owner;
        this.parentFolder = parentFolder;
        this.name = normalizeName(name);
    }

    public static Resource createFolder(Account owner, Resource parentFolder, String name) {
        return new Resource(owner, parentFolder, name);
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
