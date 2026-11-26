package com.cryfio.account.registration.application;

import java.time.Instant;

public interface VerificationCodeSender {
    void sendRegistrationCode(String email, String code, Instant expiresAt);
}
