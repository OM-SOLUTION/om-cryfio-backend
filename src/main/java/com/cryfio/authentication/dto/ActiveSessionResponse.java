package com.cryfio.authentication.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
        UUID sessionId,
        String deviceName,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        boolean isCurrent
) {}
