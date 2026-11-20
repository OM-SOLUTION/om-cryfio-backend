package com.cryfio.account.registration.infrastructure;

import com.cryfio.account.registration.application.VerificationCodeSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class ConsoleVerificationCodeSender implements VerificationCodeSender {
    @Override
    public void sendRegistrationCode(String email, String code, Instant expiresAt){
        log.warn("DEV EMAIL VERIFICATION: email={}, code={}, expiresAt={}", email,code,expiresAt);
    }
}
