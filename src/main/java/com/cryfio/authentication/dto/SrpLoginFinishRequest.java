package com.cryfio.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SrpLoginFinishRequest(
        @NotBlank(message = "Challenge ID is required")
        String challengeId,
        @NotBlank(message = "SRP client proof is required")
        String clientProof,
        @NotBlank(message = "Device name is required")
        @Size(max = 128, message = "Device name connot exceed 128 charaters")
        String deviceName
) {
}
