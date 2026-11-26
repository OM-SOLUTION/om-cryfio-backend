package com.cryfio.resource;

import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import com.cryfio.resource.domain.*;
import com.cryfio.resource.dto.PrepareDownloadResponse;
import com.cryfio.resource.repository.FileVersionKeyEnvelopeRepository;
import com.cryfio.resource.repository.FileVersionRepository;
import com.cryfio.resource.repository.ResourceRepository;
import com.cryfio.storage.GarageUploadUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadService {
    private final ResourceRepository resourceRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FileVersionKeyEnvelopeRepository fileVersionKeyEnvelopeRepository;
    private final GarageUploadUrlService garageUploadUrlService;

    @Transactional(readOnly = true)
    public PrepareDownloadResponse prepareDownload(UUID accountId,UUID resourceId){
        Resource file = resourceRepository.findByIdAndOwner_Id(resourceId,accountId)
                .orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"File was not found"));

        if(file.getResourceType() != ResourceType.FILE){
            throw new ApiException(ErrorCode.INVALID_REQUEST,"The resource is not a file");
        }
        FileVersion activeVersion = fileVersionRepository.findByResource_IdAndStatus(resourceId, FileVersionStatus.ACTIVE)
                .orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"File has no active version"));

        FileVersionKeyEnvelope envelope = fileVersionKeyEnvelopeRepository.findByFileVersion_IdAndRecipientAccount_Id(activeVersion.getId(),accountId)
                .orElseThrow(()->new ApiException(ErrorCode.FORBIDDEN,"No decryption envelope is available"));
        GarageUploadUrlService.PreparedDownload preparedDownload = garageUploadUrlService.preparedDownload(activeVersion.getStorageObjectKey());
        return new PrepareDownloadResponse(
                Base64.getUrlEncoder().withoutPadding().encodeToString(envelope.getEncryptedDek()),
                activeVersion.getPlaintextSizeBytes(),
                activeVersion.getEncryptedSizeBytes(),
                activeVersion.getChunkCount(),
                preparedDownload.downloadUrl(),
                preparedDownload.expiresAt()
        );
    }
}
