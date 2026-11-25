package com.cryfio.resource.repository;

import com.cryfio.resource.domain.FileVersionKeyEnvelope;
import com.cryfio.resource.domain.FileVersionKeyEnvelopeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FileVersionKeyEnvelopeRepository extends JpaRepository<FileVersionKeyEnvelope, FileVersionKeyEnvelopeId> {
}
