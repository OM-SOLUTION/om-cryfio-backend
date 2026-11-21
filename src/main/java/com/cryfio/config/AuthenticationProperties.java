package com.cryfio.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "authentication")
public class AuthenticationProperties {
    @NotBlank(message = "AUTH_FAKE_SALT_SECRET is required")
    private String fakeSaltSecret;
    @NotBlank(message = "AUTH_JWT_SECRET is required")
    private String jwtSecret;
}
