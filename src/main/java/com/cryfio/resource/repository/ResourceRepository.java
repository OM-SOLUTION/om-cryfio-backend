package com.cryfio.resource.repository;

import com.cryfio.resource.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    Optional<Resource> findByIdAndOwner_Id(UUID ressourceId, UUID ownerAccountId);
}
