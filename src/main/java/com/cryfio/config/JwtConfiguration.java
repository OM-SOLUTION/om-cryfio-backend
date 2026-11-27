package com.cryfio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfiguration {
    @Bean
    public SecretKey jwtSecretKey(AuthenticationProperties authenticationProperties) {
        byte[] secretBytes;
        try{
            secretBytes = Base64.getUrlDecoder().decode(authenticationProperties.getJwtSecret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AUTH_JWT_SECRET must be valid Base64URL",exception);
        }
        if(secretBytes.length != 32){
            throw new IllegalArgumentException("AUTH_JWT_SECRET must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey){
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
    }
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey){
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }
}
