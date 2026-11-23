package com.cryfio.authentication.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {}
