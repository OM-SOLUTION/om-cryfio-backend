package com.cryfio.account.registration.dto;

import com.cryfio.account.domain.enums.AccountStatus;

import java.util.UUID;

public record VerifyEmailResponse(
        UUID accountId,
        String email,
        AccountStatus status,
        String registrationToken
){}
