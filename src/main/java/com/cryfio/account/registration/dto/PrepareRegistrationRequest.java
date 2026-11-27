package com.cryfio.account.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrepareRegistrationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 320,message = "Email cannot exceed 320 characters")
        String email,
        @NotBlank(message = "First Name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,
        @Pattern(regexp = "^$|^[a-zA-Z0-9_]{3,30}$",message = "Username must contain 3 to 30 letters, numbers, or underscores")
        String username
) {}
