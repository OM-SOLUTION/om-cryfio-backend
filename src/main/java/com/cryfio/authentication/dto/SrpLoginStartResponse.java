package com.cryfio.authentication.dto;

public record SrpLoginStartResponse(
        String challengeId,
        String passwordKdfSalt,
        int passwordKdfMemoryKiB,
        int passwordKdfIterations,
        int passwordKdfParallelism,
        int passwordKdfOutputBytes,
        String serverPublicValue
) {
}
