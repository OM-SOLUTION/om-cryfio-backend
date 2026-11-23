package com.cryfio.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public final class TestSrpLogin {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final SRP6GroupParameters SRP_GROUP =
            SRP6StandardGroups.rfc5054_3072;

    private TestSrpLogin() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: <baseUrl> <email> <password>"
            );
        }

        String baseUrl = args[0].replaceAll("/+$", "");
        String email = args[1].trim().toLowerCase(Locale.ROOT);
        String password = args[2];

        byte[] identity = email.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        // First request obtains the account's stored salt.
        // Its challenge is intentionally unused.
        byte[] bootstrapA = BigIntegers.asUnsignedByteArray(
                384,
                SRP_GROUP.getG()
        );

        JsonNode bootstrapResponse = startLogin(
                baseUrl,
                email,
                bootstrapA
        );

        byte[] salt = decodeBase64Url(
                bootstrapResponse.get("passwordKdfSalt").asText()
        );

        // Actual SRP login request.
        SRP6Client client = new SRP6Client();

        client.init(
                SRP_GROUP,
                new SHA256Digest(),
                RANDOM
        );

        BigInteger clientPublicValue =
                client.generateClientCredentials(
                        salt,
                        identity,
                        passwordBytes
                );

        JsonNode startResponse = startLogin(
                baseUrl,
                email,
                BigIntegers.asUnsignedByteArray(
                        384,
                        clientPublicValue
                )
        );

        BigInteger serverPublicValue = new BigInteger(
                1,
                decodeBase64Url(
                        startResponse.get("serverPublicValue").asText()
                )
        );

        client.calculateSecret(serverPublicValue);

        BigInteger clientProof =
                client.calculateClientEvidenceMessage();

        JsonNode finishResponse = finishLogin(
                baseUrl,
                startResponse.get("challengeId").asText(),
                BigIntegers.asUnsignedByteArray(32, clientProof)
        );

        BigInteger serverProof = new BigInteger(
                1,
                decodeBase64Url(
                        finishResponse.get("serverProof").asText()
                )
        );

        if (!client.verifyServerEvidenceMessage(serverProof)) {
            throw new IllegalStateException(
                    "Server proof verification failed"
            );
        }

        System.out.println("SRP login successful");
        System.out.println();
        System.out.println(
                "ACCESS_TOKEN=" +
                        finishResponse.get("accessToken").asText()
        );
        System.out.println();
        System.out.println(
                "REFRESH_TOKEN=" +
                        finishResponse.get("refreshToken").asText()
        );
    }

    private static JsonNode startLogin(
            String baseUrl,
            String email,
            byte[] clientPublicValue
    ) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();

        body.put("email", email);
        body.put(
                "clientPublicValue",
                encodeBase64Url(clientPublicValue)
        );

        return post(
                baseUrl + "/api/v1/auth/srp/start",
                body
        );
    }

    private static JsonNode finishLogin(
            String baseUrl,
            String challengeId,
            byte[] clientProof
    ) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();

        body.put("challengeId", challengeId);
        body.put(
                "clientProof",
                encodeBase64Url(clientProof)
        );
        body.put("deviceName", "Postman SRP test");

        return post(
                baseUrl + "/api/v1/auth/srp/finish",
                body
        );
    }

    private static JsonNode post(
            String url,
            ObjectNode body
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                OBJECT_MAPPER.writeValueAsString(body)
                        )
                )
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode responseBody =
                OBJECT_MAPPER.readTree(response.body());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Request failed: " + response.body()
            );
        }

        return responseBody.get("data");
    }

    private static String encodeBase64Url(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private static byte[] decodeBase64Url(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}