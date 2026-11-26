package com.cryfio.authentication.dto;

public record SrpLoginFinishResponse(
        String serverProof,
        String accessToken,
        String refreshToken
) {}
