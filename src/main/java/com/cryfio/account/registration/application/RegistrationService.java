package com.cryfio.account.registration.application;

import com.cryfio.account.domain.*;
import com.cryfio.account.domain.enums.AccountStatus;
import com.cryfio.account.registration.dto.*;
import com.cryfio.account.repository.*;
import com.cryfio.authentication.srp.SrpConfiguration;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RegistrationService {
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN =Duration.ofMinutes(1);

    private final AccountRepository accountRepository;
    private final AccountProfileRepository  accountProfileRepository;
    private final AccountEmailVerificationRepository accountEmailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationCodeSender verificationCodeSender;
    private final RegistrationTokenService registrationTokenService;
    private final AccountEncryptionMaterialRepository accountEncryptionMaterialRepository;
    private final AccountSrpCredentialRepository accountSrpCredentialRepository;


    @Transactional
    public PrepareRegistrationResponse prepare(PrepareRegistrationRequest request){
        Account account = Account.createPending(request.email());
        if(accountRepository.existsByEmail(account.getEmail())){
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        AccountProfile profile = AccountProfile.create(
                account,
                request.firstName(),
                request.lastName(),
                request.username()
        );
        if(profile.getUsername() != null && accountProfileRepository.existsByUsername(profile.getUsername())){
            throw new ApiException(ErrorCode.USERNAME_ALREADY_TAKEN);
        }
        accountRepository.save(account);
        accountProfileRepository.save(profile);
        Instant now = Instant.now();
        String verificationCode = verificationCodeGenerator.generate();
        AccountEmailVerification verification = AccountEmailVerification.create(
                account,
                passwordEncoder.encode(verificationCode),
                now.plus(VERIFICATION_CODE_TTL),
                now.plus(RESEND_COOLDOWN)
        );
        accountEmailVerificationRepository.save(verification);
        verificationCodeSender.sendRegistrationCode(account.getEmail(),verificationCode,verification.getExpiresAt());

        return new PrepareRegistrationResponse(
                account.getId(),
                account.getEmail(),
                account.getStatus()
        );
    }
    @Transactional(noRollbackFor = ApiException.class)
    public VerifyEmailResponse verifyEmail(UUID accountId, VerifyEmailRequest request){
        AccountEmailVerification verification = accountEmailVerificationRepository.findById(accountId)
                .orElseThrow(()-> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Email verification was not found"));
        Instant now = Instant.now();

        if(verification.getVerifiedAt() != null){
            throw new ApiException(ErrorCode.CONFLICT, "Email is already verified");
        }

        if (verification.isExpired(now)) {
            throw new ApiException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (verification.hasTooManyFailedAttempts()) {
            throw new ApiException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        }

        if (!passwordEncoder.matches(request.code(), verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new ApiException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        verification.markVerified(now);

        Account account = verification.getAccount();
        account.markEmailVerified(now);
        String registrationToken  = registrationTokenService.issue(account.getId());
        return new VerifyEmailResponse(
                account.getId(),
                account.getEmail(),
                account.getStatus(),
                registrationToken
        );
    }
    @Transactional
    public CompleteRegistrationResponse uploadEncryptionMaterial(String registrationToken, UploadEncryptionMaterialRequest request) {
        UUID accountId = registrationTokenService
                .findAccountId(registrationToken)
                .orElseThrow(() -> new ApiException(ErrorCode.REGISTRATION_TOKEN_INVALID));


        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account was not found"));

        if (account.getEmailVerifiedAt() == null) {
            throw new ApiException(ErrorCode.CONFLICT, "Email must be verified before uploading encryption material");
        }

        if (account.getStatus() != AccountStatus.PENDING_REGISTRATION) {
            throw new ApiException(ErrorCode.CONFLICT, "Registration has already been completed");
        }

        if (accountEncryptionMaterialRepository.existsById(accountId)) {
            throw new ApiException(ErrorCode.CONFLICT, "Encryption material already exists for this account");
        }

        UploadEncryptionMaterialRequest.EncryptionIdentity identity = request.encryptionIdentity();

        UploadEncryptionMaterialRequest.PrivateKeyBackup backup = request.privateKeyBackup();

        UploadEncryptionMaterialRequest.Kdf kdf = backup.kdf();

        UploadEncryptionMaterialRequest.Cipher cipher = backup.cipher();

        UploadEncryptionMaterialRequest.SrpCredentials srpCredentials = request.srpCredentials();
        validateSrpConfiguration(srpCredentials);
        byte[] identityPublicKey = Base64ValueDecoder.decodeExactly(identity.publicKey(), "encryptionIdentity.publicKey", 32);

        byte[] backupPublicKey = Base64ValueDecoder.decodeExactly(backup.publicKey(), "privateKeyBackup.publicKey", 32);

        if (!Arrays.equals(identityPublicKey, backupPublicKey)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "The identity and backup public keys must match");
        }

        byte[] kdfSalt = Base64ValueDecoder.decodeAtLeast(kdf.salt(), "privateKeyBackup.kdf.salt", 16);
        byte[] cipherNonce = Base64ValueDecoder.decodeExactly(cipher.nonce(), "privateKeyBackup.cipher.nonce", 12);
        byte[] cipherCiphertext = Base64ValueDecoder.decodeAtLeast(cipher.ciphertext(), "privateKeyBackup.cipher.ciphertext", 1);

        byte[] cipherTag = Base64ValueDecoder.decodeExactly(cipher.tag(), "privateKeyBackup.cipher.tag", 16);
        byte[] srpVerifier = Base64ValueDecoder.decodeExactly(srpCredentials.verifier(),"srpCredentials.verifier", SrpConfiguration.VERIFIER_BYTES);

        AccountEncryptionMaterial encryptionMaterial = AccountEncryptionMaterial.create(
                        account,
                        request.schemaVersion(),
                        identity.algorithm(),
                        identityPublicKey,
                        backup.version(),
                        backup.algorithm(),
                        backupPublicKey,
                        kdf.algorithm(),
                        kdfSalt,
                        kdf.memoryKiB(),
                        kdf.iterations(),
                        kdf.parallelism(),
                        kdf.outputBytes(),
                        cipher.algorithm(),
                        cipherNonce,
                        cipherCiphertext,
                        cipherTag
                );

        accountEncryptionMaterialRepository.save(encryptionMaterial);
        AccountSrpCredential srpCredential  = AccountSrpCredential.create(
                account,
                srpCredentials.protocol(),
                srpCredentials.groupId(),
                srpCredentials.hashAlgorithm(),
                srpVerifier,
                kdf.algorithm(),
                kdfSalt,
                kdf.memoryKiB(),
                kdf.iterations(),
                kdf.parallelism(),
                kdf.outputBytes()
        );
        accountSrpCredentialRepository.save(srpCredential);

        Instant activatedAt = Instant.now();
        account.activate(activatedAt);

        revokeTokenAfterSuccessfulCommit(registrationToken);

        return new CompleteRegistrationResponse(
                account.getId(),
                account.getStatus(),
                activatedAt
        );
    }

    private void revokeTokenAfterSuccessfulCommit(String registrationToken){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit(){
                        registrationTokenService.revoke(registrationToken);
                    }
                }
        );
    }
    private void validateSrpConfiguration(UploadEncryptionMaterialRequest.SrpCredentials  srpCredentials){
        if(!SrpConfiguration.PROTOCOL.equals(srpCredentials.protocol())){
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported SRP group");
        }
        if (!SrpConfiguration.GROUP_ID.equals(srpCredentials.groupId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported SRP group");
        }
        if(!SrpConfiguration.HASH_ALGORITHM.equals(srpCredentials.hashAlgorithm())){
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported SRP hash algorithm");
        }
    }
}
