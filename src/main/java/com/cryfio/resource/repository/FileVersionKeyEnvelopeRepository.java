package com.cryfio.resource.repository;

import com.cryfio.resource.domain.FileVersionKeyEnvelope;
import com.cryfio.resource.domain.FileVersionKeyEnvelopeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface FileVersionKeyEnvelopeRepository extends JpaRepository<FileVersionKeyEnvelope, FileVersionKeyEnvelopeId> {
    Optional<FileVersionKeyEnvelope> findByFileVersion_IdAndRecipientAccount_Id(UUID fileVersionId,UUID recipientAccountId);
}
