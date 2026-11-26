package com.cryfio.tools;

import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;
import org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups;
import org.bouncycastle.util.BigIntegers;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public final class GeneratePostmanRegistrationPayload {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SRP6GroupParameters SRP_GROUP =
            SRP6StandardGroups.rfc5054_3072;

    private GeneratePostmanRegistrationPayload() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: <email> <password>"
            );
        }

        String email = args[0].trim().toLowerCase(Locale.ROOT);
        String password = args[1];

        byte[] salt = randomBytes(16);

        SRP6VerifierGenerator verifierGenerator =
                new SRP6VerifierGenerator();

        verifierGenerator.init(
                SRP_GROUP.getN(),
                SRP_GROUP.getG(),
                new SHA256Digest()
        );

        byte[] verifier = BigIntegers.asUnsignedByteArray(
                384,
                verifierGenerator.generateVerifier(
                        salt,
                        email.getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8)
                )
        );

        String publicKey = base64Url(randomBytes(32));

        String payload = """
                {
                  "schemaVersion": 1,
                  "encryptionIdentity": {
                    "algorithm": "X25519",
                    "publicKey": "%s"
                  },
                  "privateKeyBackup": {
                    "version": 1,
                    "algorithm": "X25519+Argon2id+HKDF-SHA256+AES-256-GCM",
                    "publicKey": "%s",
                    "kdf": {
                      "algorithm": "Argon2id",
                      "salt": "%s",
                      "memoryKiB": 65536,
                      "iterations": 3,
                      "parallelism": 4,
                      "outputBytes": 32
                    },
                    "cipher": {
                      "algorithm": "AES-256-GCM",
                      "nonce": "%s",
                      "ciphertext": "%s",
                      "tag": "%s"
                    }
                  },
                  "srpCredentials": {
                    "protocol": "SRP-6a",
                    "groupId": "RFC5054_3072",
                    "hashAlgorithm": "SHA-256",
                    "verifier": "%s"
                  }
                }
                """.formatted(
                publicKey,
                publicKey,
                base64Url(salt),
                base64Url(randomBytes(12)),
                base64Url(randomBytes(64)),
                base64Url(randomBytes(16)),
                base64Url(verifier)
        );

        System.out.println(payload);
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}