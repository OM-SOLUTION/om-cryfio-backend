package com.cryfio.authentication.srp;

import com.cryfio.config.AuthenticationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class FakeSaltService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SALT_BYTES = 16;
    private final AuthenticationProperties authenticationProperties;

    public byte[] derive(String normalizeEmail){
        try{
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(authenticationProperties.getFakeSaltSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] fullHash = mac.doFinal(normalizeEmail.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(fullHash, SALT_BYTES);
        }catch(NoSuchAlgorithmException | InvalidKeyException exception){
            throw new IllegalStateException("Could not derive deterministic fake salt",exception);
        }
    }
}
