package com.cryfio.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST,"Request validation failed"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"INVALID REQUEST"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT,"An account with this email already exists"),
    USERNAME_ALREADY_TAKEN(HttpStatus.CONFLICT,"This username is already taken"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"Authentication is required"),
    FORBIDDEN(HttpStatus.FORBIDDEN,"Access Denied"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "Verification code is invalid"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.GONE, "Verification code has expired"),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid verification attempts"),
    REGISTRATION_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"Registration token is invalid or Expired"),
    ;
    private final HttpStatus status;
    private final String defaultMessage;
    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
