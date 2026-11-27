package com.cryfio.authentication;

import com.cryfio.account.domain.Account;
import com.cryfio.account.domain.AccountSession;
import com.cryfio.account.domain.AccountSrpCredential;
import com.cryfio.account.domain.enums.AccountStatus;
import com.cryfio.account.repository.AccountRepository;
import com.cryfio.account.repository.AccountSessionRepository;
import com.cryfio.account.repository.AccountSrpCredentialRepository;
import com.cryfio.authentication.dto.SrpLoginFinishRequest;
import com.cryfio.authentication.dto.SrpLoginFinishResponse;
import com.cryfio.authentication.dto.SrpLoginStartRequest;
import com.cryfio.authentication.dto.SrpLoginStartResponse;
import com.cryfio.authentication.session.SessionRegistryService;
import com.cryfio.authentication.session.SessionTokenService;
import com.cryfio.authentication.srp.FakeSaltService;
import com.cryfio.authentication.srp.SrpConfiguration;
import com.cryfio.authentication.srp.SrpLoginChallengeService;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.srp.SRP6Util;
import org.bouncycastle.util.BigIntegers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SrpAuthenticationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String KDF_ALGORITHM = "Argon2id";
    private static final int KDF_MEMORY_KIB = 65536;
    private static final int KDF_ITERATIONS = 3;
    private static final int KDF_PARALLELISM = 4;
    private static final int KDF_OUTPUT_BYTES = 32;

    private final AccountRepository accountRepository;
    private final AccountSrpCredentialRepository accountSrpCredentialRepository;
    private final FakeSaltService fakeSaltService;
    private final SrpLoginChallengeService srpLoginChallengeService;
    private final AccountSessionRepository accountSessionRepository;
    private final SessionTokenService sessionTokenService;
    private final SessionRegistryService sessionRegistryService;

    public SrpLoginStartResponse start(SrpLoginStartRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        byte[] clientPublicValueBytes = decodePublicValue(request.clientPublicValue());

        BigInteger clientPublicValue = validateClientPublicValue(clientPublicValueBytes);

        LoginMaterial loginMaterial = findLoginMaterial(normalizedEmail);

        BigInteger verifier = new BigInteger(1, loginMaterial.srpVerifier());

        BigInteger serverPrivateValue = SRP6Util.generatePrivateValue(
                SrpConfiguration.newDigest(),
                SrpConfiguration.GROUP.getN(),
                SrpConfiguration.GROUP.getG(),
                SECURE_RANDOM
        );

        BigInteger multiplier = SRP6Util.calculateK(
                SrpConfiguration.newDigest(),
                SrpConfiguration.GROUP.getN(),
                SrpConfiguration.GROUP.getG()
        );

        BigInteger serverPublicValue = multiplier
                .multiply(verifier)
                .mod(SrpConfiguration.GROUP.getN())
                .add(SrpConfiguration.GROUP.getG().modPow(serverPrivateValue,SrpConfiguration.GROUP.getN()))
                .mod(SrpConfiguration.GROUP.getN());

        String challengeId = srpLoginChallengeService.create(
                loginMaterial.accountId(),
                toFixedLengthBytes(clientPublicValue),
                toFixedLengthBytes(serverPrivateValue),
                toFixedLengthBytes(serverPublicValue)
        );

        return new SrpLoginStartResponse(
                challengeId,
                encode(loginMaterial.passwordKdfSalt()),
                loginMaterial.passwordKdfMemoryKiB(),
                loginMaterial.passwordKdfIterations(),
                loginMaterial.passwordKdfParallelism(),
                loginMaterial.passwordKdfOutputBytes(),
                encode(toFixedLengthBytes(serverPublicValue))
        );
    }
    @Transactional
    public SrpLoginFinishResponse finish(SrpLoginFinishRequest request) {
        SrpLoginChallengeService.Challenge challenge = srpLoginChallengeService.consume(request.challengeId())
                        .orElseThrow(this::invalidCredentials);

        if (challenge.accountId() == null) {
            throw invalidCredentials();
        }

        Account account = accountRepository.findById(challenge.accountId())
                .filter(value -> value.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(this::invalidCredentials);

        AccountSrpCredential credential = accountSrpCredentialRepository.findById(account.getId())
                        .orElseThrow(this::invalidCredentials);

        BigInteger clientPublicValue = new BigInteger(1,decodeBase64Url(challenge.clientPublicValue(),"Stored SRP client public value",SrpConfiguration.VERIFIER_BYTES));

        BigInteger serverPrivateValue = new BigInteger(1,decodeBase64Url(challenge.serverPrivateValue(), "Stored SRP server private value", SrpConfiguration.VERIFIER_BYTES));

        BigInteger serverPublicValue = new BigInteger(1,decodeBase64Url(challenge.serverPublicValue(), "Stored SRP server public value", SrpConfiguration.VERIFIER_BYTES));

        BigInteger verifier = new BigInteger(1,credential.getSrpVerifier());

        BigInteger scramblingParameter = SRP6Util.calculateU(
                SrpConfiguration.newDigest(),
                SrpConfiguration.GROUP.getN(),
                clientPublicValue,
                serverPublicValue
        );

        BigInteger sharedSecret = verifier
                .modPow(scramblingParameter, SrpConfiguration.GROUP.getN())
                .multiply(clientPublicValue)
                .mod(SrpConfiguration.GROUP.getN())
                .modPow(serverPrivateValue, SrpConfiguration.GROUP.getN());

        byte[] submittedClientProof = decodeBase64Url(
                request.clientProof(),
                "SRP client proof",
                SrpConfiguration.PROOF_BYTES
        );

        BigInteger expectedClientProof = SRP6Util.calculateM1(
                SrpConfiguration.newDigest(),
                SrpConfiguration.GROUP.getN(),
                clientPublicValue,
                serverPublicValue,
                sharedSecret
        );

        if (!MessageDigest.isEqual(toProofBytes(expectedClientProof), submittedClientProof)) {
            throw invalidCredentials();
        }

        BigInteger serverProof = SRP6Util.calculateM2(
                SrpConfiguration.newDigest(),
                SrpConfiguration.GROUP.getN(),
                clientPublicValue,
                expectedClientProof,
                sharedSecret
        );

        Instant now = Instant.now();

        SessionTokenService.RefreshToken refreshToken =
                sessionTokenService.createRefreshToken(now);

        AccountSession session = AccountSession.create(
                account,
                refreshToken.hash(),
                request.deviceName(),
                refreshToken.expiresAt()
        );

        accountSessionRepository.save(session);

        activateSessionAfterSuccessfulCommit(
                session.getId(),
                account.getId(),
                refreshToken.expiresAt()
        );

        String accessToken = sessionTokenService.createAccessToken(
                account.getId(),
                session.getId(),
                now
        );

        return new SrpLoginFinishResponse(
                encode(toProofBytes(serverProof)),
                accessToken,
                refreshToken.value()
        );
    }


    private LoginMaterial findLoginMaterial(String normalizedEmail) {
        Optional<Account> account = accountRepository.findByEmail(normalizedEmail);

        if (account.isEmpty() || account.get().getStatus() != AccountStatus.ACTIVE) {
            return fakeLoginMaterial(normalizedEmail);
        }

        Optional<AccountSrpCredential> credential = accountSrpCredentialRepository.findById(account.get().getId());

        if (credential.isEmpty()) {
            return fakeLoginMaterial(normalizedEmail);
        }

        AccountSrpCredential value = credential.get();

        return new LoginMaterial(
                account.get().getId(),
                value.getPasswordKdfSalt(),
                value.getPasswordKdfMemoryKiB(),
                value.getPasswordKdfIterations(),
                value.getPasswordKdfParallelism(),
                value.getPasswordKdfOutputBytes(),
                value.getSrpVerifier()
        );
    }

    private LoginMaterial fakeLoginMaterial(String normalizedEmail) {
        return new LoginMaterial(
                null,
                fakeSaltService.derive(normalizedEmail),
                KDF_MEMORY_KIB,
                KDF_ITERATIONS,
                KDF_PARALLELISM,
                KDF_OUTPUT_BYTES,
                toFixedLengthBytes(SrpConfiguration.GROUP.getG())
        );
    }

    private BigInteger validateClientPublicValue(byte[] value) {
        try {
            return SRP6Util.validatePublicValue(SrpConfiguration.GROUP.getN(), new BigInteger(1, value));
        } catch (CryptoException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid SRP client public value");
        }
    }

    private byte[] decodePublicValue(String value) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);

            if (decoded.length != SrpConfiguration.VERIFIER_BYTES) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "SRP client public value must contain exactly "  + SrpConfiguration.VERIFIER_BYTES + " bytes");
            }

            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "SRP client public value must be valid Base64URL");
        }
    }

    private static byte[] toFixedLengthBytes(BigInteger value) {
        return BigIntegers.asUnsignedByteArray(SrpConfiguration.VERIFIER_BYTES, value);
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
    private byte[] decodeBase64Url(String value, String fieldName, int expectedBytes){
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            if(decoded.length != expectedBytes) {
                throw invalidCredentials();
            }
            return decoded;
        }catch (IllegalArgumentException exception){
            throw invalidCredentials();
        }
    }
    private static byte[] toProofBytes(BigInteger value){
        return BigIntegers.asUnsignedByteArray(SrpConfiguration.PROOF_BYTES, value);
    }

    private ApiException invalidCredentials(){
        return new ApiException(ErrorCode.UNAUTHORIZED,"Invalid email or password");
    }
    private void activateSessionAfterSuccessfulCommit(UUID sessionId, UUID accountId, Instant expiresAt) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sessionRegistryService.activate(sessionId, accountId, expiresAt);
                    }
                }
        );
    }
    private record LoginMaterial(
            UUID accountId,
            byte[] passwordKdfSalt,
            int passwordKdfMemoryKiB,
            int passwordKdfIterations,
            int passwordKdfParallelism,
            int passwordKdfOutputBytes,
            byte[] srpVerifier
    ) {}
}