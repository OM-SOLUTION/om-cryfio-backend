package com.cryfio.resource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record InitializeUploadRequest(
        UUID parentFolderId,

        @NotBlank(message="File name is required")
        @Size(max = 255, message = "File name cannot exceed 255 characters")
        String fileName,
        @Min(value = 0, message = "Plaintext size cannot be negative")
        @Max(value = 2_147_483_648L, message = "File size cannot exceed 2 GiB")
        long plaintextSizeBytes,

        @Positive(message ="encrypted size must be greater then zero")
        long encryptedSizeBytes,
        @NotBlank(message = "Ciphertext SHA-256 is required")
        String ciphertextSha256,

        @Min(value = 1, message = "Chunk count must be at least 1")
        int chunkCount,

        @NotEmpty(message = "At least one encrypted DEK envelope is required")
        List<KeyEnvelopeRequest> keyEnvelopes
){
        public record KeyEnvelopeRequest(
                @NotNull(message = "Envelope recipient account ID is required")
                UUID recipientAccountId,

                @NotBlank(message = "Encrypted DEK is required")
                String encryptedDek
        ){}
}
