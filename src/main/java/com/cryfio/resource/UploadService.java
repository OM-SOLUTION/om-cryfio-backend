package com.cryfio.resource;

import com.cryfio.account.domain.Account;
import com.cryfio.account.registration.application.Base64ValueDecoder;
import com.cryfio.account.repository.AccountRepository;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import com.cryfio.resource.domain.*;
import com.cryfio.resource.dto.CompleteUploadRequest;
import com.cryfio.resource.dto.CompleteUploadResponse;
import com.cryfio.resource.dto.InitializeUploadRequest;
import com.cryfio.resource.dto.InitializeUploadResponse;
import com.cryfio.resource.repository.FileVersionKeyEnvelopeRepository;
import com.cryfio.resource.repository.FileVersionRepository;
import com.cryfio.resource.repository.ResourceRepository;
import com.cryfio.storage.GarageUploadUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final AccountRepository accountRepository;
    private final ResourceRepository resourceRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FileVersionKeyEnvelopeRepository fileVersionKeyEnvelopeRepository;
    private final GarageUploadUrlService garageUploadUrlService;

    @Transactional
    public InitializeUploadResponse initializeUpload(UUID accountId, InitializeUploadRequest request){
        Account uploader = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        Resource parentFolder = findDestinationFolder(request.parentFolderId(),accountId);
        Account owner = parentFolder == null ? uploader : parentFolder.getOwner();
        checkQuota(owner, request.encryptedSizeBytes());
        // NOTE: Sharing is not implemented yet, so only the Owner may receive an envelope in this first upload flow.
        byte[] ownerEnvelope = decodeOwnerEnvelope(request,owner);
        Resource file = Resource.createFile(
                owner,
                uploader,
                parentFolder,
                request.fileName(),
                extractFileExtension(request.fileName())
        );
        resourceRepository.save(file);
        String storageObjectKey = "objects/" + UUID.randomUUID();
        GarageUploadUrlService.PreparedUpload preparedUpload = garageUploadUrlService.prepareUpload(
                storageObjectKey,
                request.encryptedSizeBytes()
        );
        FileVersion fileVersion = FileVersion.createPending(
                file,
                1,
                storageObjectKey,
                preparedUpload.expiresAt(),
                request.plaintextSizeBytes(),
                request.encryptedSizeBytes(),
                Base64ValueDecoder.decodeExactly(request.ciphertextSha256(),"ciphertextSha256",32),
                request.chunkCount(),
                uploader
        );
        if(preparedUpload.multipart()){
            fileVersion.setMultipartUploadId(preparedUpload.multipartUploadId());
        }
        fileVersionRepository.save(fileVersion);
        fileVersionKeyEnvelopeRepository.save(FileVersionKeyEnvelope.create(fileVersion,owner,ownerEnvelope));
        List<InitializeUploadResponse.UploadPartResponse> uploadParts = preparedUpload.uploadParts()
                .stream().map(part -> new InitializeUploadResponse.UploadPartResponse(
                        part.partNumber(),
                        part.expectedSizeBytes(),
                        part.uploadUrl()
                )).toList();
        return new InitializeUploadResponse(
                file.getId(),
                fileVersion.getId(),
                preparedUpload.multipart(),
                GarageUploadUrlService.MULTIPART_PART_SIZE_BYTES,
                preparedUpload.expiresAt(),
                uploadParts
        );
    }
    @Transactional
    public CompleteUploadResponse completeUpload(UUID accountId, UUID versionId, CompleteUploadRequest request){
        FileVersion fileVersion = fileVersionRepository.findByIdAndResource_Owner_Id(versionId,accountId)
                .orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Upload was not found"));
        if(fileVersion.getStatus() != FileVersionStatus.PENDING_UPLOAD){
            throw new ApiException(ErrorCode.CONFLICT,"Upload is not pending");
        }
        if(fileVersion.getMultipartUploadId() == null){
            if(!request.parts().isEmpty()){
                throw new ApiException(ErrorCode.INVALID_REQUEST,"A single-part upload must not contain multipart parts");
            }
        }else {
            completeMultipartUpload(fileVersion,request);
        }
        boolean ciphertextMatches;
        try{
            ciphertextMatches = garageUploadUrlService.matchesExpectedObject(
                    fileVersion.getStorageObjectKey(),
                    fileVersion.getEncryptedSizeBytes(),
                    fileVersion.getCiphertextSha256()
            );
        }catch (S3Exception exception){
            throw new ApiException(ErrorCode.CONFLICT,"Uploaded ciphertext could not be verified");
        }
        if(!ciphertextMatches){
            throw new ApiException(ErrorCode.CONFLICT,"Uploaded ciphertext size or SHA-256 does not match");
        }
        Instant activatedAt = Instant.now();
        fileVersion.activate(activatedAt);

        return new CompleteUploadResponse(
                fileVersion.getResource().getId(),
                fileVersion.getId(),
                activatedAt
        );
    }
    private void completeMultipartUpload(FileVersion fileVersion,CompleteUploadRequest request){
        int expectedPartCount = Math.toIntExact(
                ((fileVersion.getEncryptedSizeBytes() - 1) / GarageUploadUrlService.MULTIPART_PART_SIZE_BYTES) + 1);
        if(request.parts().size() != expectedPartCount){
            throw new ApiException(ErrorCode.INVALID_REQUEST,"Incorrect number of multipart upload parts");
        }
        Set<Integer> partNumbers = new HashSet<>();
        for(CompleteUploadRequest.CompletedUploadRequest part: request.parts()){
            if(!partNumbers.add(part.partNumber())){
                throw new ApiException(ErrorCode.INVALID_REQUEST,"Multipart upload parts are invalid");
            }
        }
        garageUploadUrlService.completeMultipartUpload(
                fileVersion.getStorageObjectKey(),
                fileVersion.getMultipartUploadId(),
                request.parts()
                        .stream()
                        .map(part -> new GarageUploadUrlService.CompletedUploadPart(part.partNumber(), part.eTag()))
                        .toList()
        );
    }

    private Resource findDestinationFolder(UUID parentFolderId,UUID accountId){
        if(parentFolderId == null) return null;

        Resource parentFolder = resourceRepository.findByIdAndOwner_Id(parentFolderId,accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Destination folder was not found"));
        if (parentFolder.getResourceType() != ResourceType.FOLDER) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Destination resource must be a folder");
        }
        return parentFolder;
    }
    private void checkQuota(Account owner,long incomingEncryptedBytes){
        long reservedOrUsedBytes = fileVersionRepository.sumEncryptedBytesByOwnerAndStatusIn(
                owner.getId(),
                List.of(FileVersionStatus.PENDING_UPLOAD,FileVersionStatus.ACTIVE)
        );
        if(reservedOrUsedBytes > owner.getQuotaBytes() || incomingEncryptedBytes > owner.getQuotaBytes() - reservedOrUsedBytes){
            throw new ApiException(ErrorCode.CONFLICT, "Storage quota would be exceeded");
        }
    }

    private byte[] decodeOwnerEnvelope(InitializeUploadRequest request,Account owner){
        if(request.keyEnvelopes().size() != 1){
            throw new ApiException(ErrorCode.INVALID_REQUEST,"Exactly one Owner envelope is required until sharing is implemented");
        }
        InitializeUploadRequest.KeyEnvelopeRequest envelope = request.keyEnvelopes().getFirst();
        if(!owner.getId().equals(envelope.recipientAccountId())){
            throw new ApiException(ErrorCode.UNAUTHORIZED,"The envelope recipient must be the file Owner");
        }
        return Base64ValueDecoder.decodeAtLeast(envelope.encryptedDek(),"keyEnvelopes[0].encryptedDek",1);
    }
    private static String extractFileExtension(String fileName) {
        String normalizeName =fileName.trim();
        int finalDot = normalizeName.lastIndexOf('.');
        if(finalDot <= 0 || finalDot == normalizeName.length()-1){
            return null;
        }
        return normalizeName.substring(finalDot + 1).toLowerCase();
    }
}
