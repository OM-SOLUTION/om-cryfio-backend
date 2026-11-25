package com.cryfio.resource.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InitializeUploadResponse(
        UUID resourceId,
        UUID versionId,
        boolean multipart,
        long partSizeBytes,
        Instant uploadUrlExpiresAt,
        List<UploadPartResponse> uploadParts
) {
    public record UploadPartResponse(
            int partNumber,
            long expectedSizeBytes,
            String uploadUrl
    ) {}
}
