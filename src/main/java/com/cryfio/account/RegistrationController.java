package com.cryfio.account;

import com.cryfio.account.registration.dto.*;
import com.cryfio.account.registration.application.RegistrationService;
import com.cryfio.common.api.ApiResponse;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    @PostMapping
    public ResponseEntity<ApiResponse<PrepareRegistrationResponse>> prepareRegistration(
            @Valid @RequestBody PrepareRegistrationRequest request
    ){
        PrepareRegistrationResponse result = registrationService.prepare(request);
        ApiResponse<PrepareRegistrationResponse> response = ApiResponse.success(HttpStatus.CREATED.value(),"REGISTRATION_PREPARED","Registration prepared successfully",result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/{accountId}/email-verification")
    public ResponseEntity<ApiResponse<VerifyEmailResponse>> verifyEmail(
            @PathVariable UUID accountId,
            @Valid @RequestBody VerifyEmailRequest request
    ) {
      VerifyEmailResponse result = registrationService.verifyEmail(accountId, request);
      ApiResponse<VerifyEmailResponse> response = ApiResponse.success(
              HttpStatus.OK.value(),"EMAIL_VERIFIED","Email verified successfully",result
      );
      return ResponseEntity.ok(response);
    }
    @PutMapping("encryption-material")
    public ResponseEntity<ApiResponse<CompleteRegistrationResponse>> uploadEncryptionMaterial(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION,required = false) String authorizationHeader,
            @Valid @RequestBody UploadEncryptionMaterialRequest request
    ){
        String registrationToken = extractBearerToken(authorizationHeader);
        CompleteRegistrationResponse result = registrationService.uploadEncryptionMaterial(registrationToken, request);
        ApiResponse<CompleteRegistrationResponse> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "REGISTRATION_COMPLETED",
                "Registration completed successfully",
                result
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractBearerToken(String authorizationHeader){
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new ApiException(ErrorCode.REGISTRATION_TOKEN_INVALID);
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if(token.isBlank()){
            throw new ApiException(ErrorCode.REGISTRATION_TOKEN_INVALID);
        }
        return token;
    }
}
