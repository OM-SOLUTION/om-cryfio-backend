package com.cryfio.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record CompleteUploadResponse(
        UUID resourceId,
        UUID versionId,
        Instant activatedAt
) {}
