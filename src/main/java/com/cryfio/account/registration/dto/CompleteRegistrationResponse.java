package com.cryfio.account.registration.dto;

import com.cryfio.account.domain.enums.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record CompleteRegistrationResponse(
        UUID accountId,
        AccountStatus status,
        Instant activatedAt
) {}
