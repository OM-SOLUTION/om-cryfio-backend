package com.cryfio.account.registration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record UploadEncryptionMaterialRequest(
        @Min(value = 1, message = "schemaVersion must be 1")
        @Max(value = 1, message = "schemaVersion must be 1")
        short schemaVersion,

        @NotNull(message = "encryptionIdentity is required")
        @Valid
        EncryptionIdentity encryptionIdentity,

        @NotNull(message = "privateKeyBackup is required")
        @Valid
        PrivateKeyBackup privateKeyBackup
) {
    public record EncryptionIdentity(
            @NotBlank(message = "encryptionIdentity.algorithm is required")
            @Pattern(regexp = "^X25519$", message = "encryptionIdentity.algorithm must be X25519")
            String algorithm,

            @NotBlank(message = "encryptionIdentity.publicKey is required")
            String publicKey
    ){}
    public record PrivateKeyBackup(
            @Min(value = 1, message = "privateKeyBackup.version must be 1")
            @Max(value = 1, message = "privateKeyBackup.version must be 1")
            short version,

            @NotBlank(message = "privateKeyBackup.algorithm is required")
            @Pattern(regexp = "^X25519\\+Argon2id\\+HKDF-SHA256\\+AES-256-GCM$", message = "privateKeyBackup.algorithm is not supported")
            String algorithm,

            @NotBlank(message = "privateKeyBackup.publicKey is required")
            String publicKey,

            @NotNull(message = "privateKeyBackup.kdf is required")
            @Valid
            Kdf kdf,

            @NotNull(message = "privateKeyBackup.cipher is required")
            @Valid
            Cipher cipher
    ) {}

    public record Kdf(

            @NotBlank(message = "kdf.algorithm is required")
            @Pattern(regexp = "^Argon2id$", message = "kdf.algorithm must be Argon2id")
            String algorithm,

            @NotBlank(message = "kdf.salt is required")
            String salt,

            @Min(value = 65536, message = "kdf.memoryKiB must be 65536")
            @Max(value = 65536, message = "kdf.memoryKiB must be 65536")
            int memoryKiB,

            @Min(value = 3, message = "kdf.iterations must be 3")
            @Max(value = 3, message = "kdf.iterations must be 3")
            int iterations,

            @Min(value = 4, message = "kdf.parallelism must be 4")
            @Max(value = 4, message = "kdf.parallelism must be 4")
            int parallelism,

            @Min(value = 32, message = "kdf.outputBytes must be 32")
            @Max(value = 32, message = "kdf.outputBytes must be 32")
            int outputBytes
    ) {}
    public record Cipher(

            @NotBlank(message = "cipher.algorithm is required")
            @Pattern(regexp = "^AES-256-GCM$", message = "cipher.algorithm must be AES-256-GCM"
            )
            String algorithm,

            @NotBlank(message = "cipher.nonce is required")
            String nonce,

            @NotBlank(message = "cipher.ciphertext is required")
            String ciphertext,

            @NotBlank(message = "cipher.tag is required")
            String tag
    ) {
    }
}
