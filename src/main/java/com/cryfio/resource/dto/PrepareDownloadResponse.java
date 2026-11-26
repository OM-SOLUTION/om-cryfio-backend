package com.cryfio.resource.dto;

import java.time.Instant;

public record PrepareDownloadResponse(
        String encryptedDek,
        long plaintextSizeBytes,
        long encryptedSizeBytes,
        int chunkCount,
        String downloadUrl,
        Instant downloadUrlExpiresAt
) {}
