package com.cryfio.storage;

import com.cryfio.config.GarageStorageProperties;
import com.cryfio.resource.dto.CompleteUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GarageUploadUrlService {

    public static final long MULTIPART_PART_SIZE_BYTES = 16L * 1024L * 1024L;
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final S3Client garageS3Client;
    private final S3Presigner garageS3Presigner;
    private final GarageStorageProperties garageStorageProperties;

    public PreparedUpload prepareUpload(String storageObjectKey, long encryptedSizeBytes) {
        Instant expiresAt = Instant.now().plus(UPLOAD_URL_TTL);

        if (encryptedSizeBytes <= MULTIPART_PART_SIZE_BYTES) {
            return prepareSinglePartUpload(storageObjectKey, encryptedSizeBytes, expiresAt);
        }

        return prepareMultipartUpload(
                storageObjectKey,
                encryptedSizeBytes,
                expiresAt
        );
    }
    public void completeMultipartUpload(String storageObjectKey, String multipartUploadId, List<CompletedUploadPart> uploadedParts) {
        List<CompletedPart> completedParts = uploadedParts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build()
                ).toList();
        garageS3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(garageStorageProperties.getBucket())
                        .key(storageObjectKey)
                        .uploadId(multipartUploadId)
                        .multipartUpload(
                                CompletedMultipartUpload.builder()
                                        .parts(completedParts)
                                        .build()
                        )
                        .build()
        );
    }
    public boolean matchesExpectedObject(String storageObjectKey,long expectedSizeBytes,byte[] expectedSha256) {
        HeadObjectResponse metadata = garageS3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(garageStorageProperties.getBucket())
                        .key(storageObjectKey)
                        .build()
        );
        if(metadata.contentLength() == null || metadata.contentLength() != expectedSizeBytes){
         return false;
        }
        try{
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try(ResponseInputStream<?> objectStream = garageS3Client.getObject(GetObjectRequest.builder()
                    .bucket(garageStorageProperties.getBucket())
                    .key(storageObjectKey)
                    .build())
            ){
                byte[] buffer = new byte[8192];
                int read;
                while((read = objectStream.read(buffer)) != -1){
                    sha256.update(buffer, 0, read);
                }
            }
            return MessageDigest.isEqual(expectedSha256,sha256.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded ciphertext from Garage", exception);
        }
    }
    private PreparedUpload prepareSinglePartUpload(String storageObjectKey, long encryptedSizeBytes, Instant expiresAt) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(garageStorageProperties.getBucket())
                        .key(storageObjectKey)
                        .build();

        PresignedPutObjectRequest presignedRequest = garageS3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                                .signatureDuration(UPLOAD_URL_TTL)
                                .putObjectRequest(putObjectRequest)
                                .build());

        return new PreparedUpload(
                false,
                null,
                expiresAt,
                List.of(new UploadPartUrl(1, encryptedSizeBytes, presignedRequest.url().toString()))
        );
    }

    private PreparedUpload prepareMultipartUpload(String storageObjectKey, long encryptedSizeBytes, Instant expiresAt) {
        CreateMultipartUploadResponse multipartUpload = garageS3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                                .bucket(garageStorageProperties.getBucket())
                                .key(storageObjectKey)
                                .build());

        String multipartUploadId = multipartUpload.uploadId();

        if (multipartUploadId == null || multipartUploadId.isBlank()) {
            throw new IllegalStateException("Garage did not return a multipart upload ID");
        }

        int partCount = Math.toIntExact(((encryptedSizeBytes - 1) / MULTIPART_PART_SIZE_BYTES) + 1);

        List<UploadPartUrl> uploadParts = new ArrayList<>(partCount);

        for (int partNumber = 1;
             partNumber <= partCount;
             partNumber++) {

            long expectedSizeBytes = expectedPartSize(
                    encryptedSizeBytes,
                    partNumber
            );

            UploadPartRequest uploadPartRequest =
                    UploadPartRequest.builder()
                            .bucket(garageStorageProperties.getBucket())
                            .key(storageObjectKey)
                            .uploadId(multipartUploadId)
                            .partNumber(partNumber)
                            .build();

            PresignedUploadPartRequest presignedRequest = garageS3Presigner.presignUploadPart(
                    UploadPartPresignRequest.builder()
                                    .signatureDuration(UPLOAD_URL_TTL)
                                    .uploadPartRequest(uploadPartRequest)
                                    .build());

            uploadParts.add(new UploadPartUrl(partNumber, expectedSizeBytes, presignedRequest.url().toString()));
        }

        return new PreparedUpload(
                true,
                multipartUploadId,
                expiresAt,
                List.copyOf(uploadParts)
        );
    }
    public PreparedDownload preparedDownload(String storageObjectKey) {
        Instant expiresAt = Instant.now().plus(UPLOAD_URL_TTL);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(garageStorageProperties.getBucket())
                .key(storageObjectKey)
                .build();
        PresignedGetObjectRequest presignedGetObjectRequest = garageS3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(DOWNLOAD_URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build());
        return new PreparedDownload(
                presignedGetObjectRequest.url().toString(),
                expiresAt
        );
    }
    private static long expectedPartSize(long encryptedSizeBytes, int partNumber) {
        long bytesBeforePart = (long) (partNumber - 1) * MULTIPART_PART_SIZE_BYTES;

        return Math.min(MULTIPART_PART_SIZE_BYTES, encryptedSizeBytes - bytesBeforePart);
    }

    public record PreparedUpload(
            boolean multipart,
            String multipartUploadId,
            Instant expiresAt,
            List<UploadPartUrl> uploadParts
    ) {}

    public record UploadPartUrl(
            int partNumber,
            long expectedSizeBytes,
            String uploadUrl
    ) {}
    public record CompletedUploadPart(
            int partNumber,
            String eTag
    ){}
    public record PreparedDownload(
            String downloadUrl,
            Instant expiresAt
    ){}
}