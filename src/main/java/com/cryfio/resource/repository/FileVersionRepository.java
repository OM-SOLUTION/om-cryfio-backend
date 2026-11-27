package com.cryfio.resource.repository;

import com.cryfio.resource.domain.FileVersion;
import com.cryfio.resource.domain.FileVersionStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, UUID> {
    Optional<FileVersion> findTopByResource_IdOrderByVersionNumberDesc(UUID resourceId);
    @Query("""
        SELECT coalesce(sum(fileVersion.encryptedSizeBytes),0)
        FROM FileVersion fileVersion
        WHERE fileVersion.resource.owner.id = :ownerAccountId
        AND fileVersion.status in :statuses
    """)
    long sumEncryptedBytesByOwnerAndStatusIn(@Param("ownerAccountId") UUID ownerAccountId, @Param("statuses") Collection<FileVersionStatus> statuses);
    Optional<FileVersion> findByIdAndResource_Owner_Id(UUID versionId,UUID ownerAccountId);
}
