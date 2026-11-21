package com.cryfio.authentication;

import com.cryfio.authentication.dto.SrpLoginFinishRequest;
import com.cryfio.authentication.dto.SrpLoginFinishResponse;
import com.cryfio.authentication.dto.SrpLoginStartRequest;
import com.cryfio.authentication.dto.SrpLoginStartResponse;
import com.cryfio.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final SrpAuthenticationService srpAuthenticationService;
    @PostMapping("/srp/start")
    public ResponseEntity<ApiResponse<SrpLoginStartResponse>> startSrpLogin(
            @Valid @RequestBody SrpLoginStartRequest request
    ){
        SrpLoginStartResponse result = srpAuthenticationService.start(request);
        ApiResponse<SrpLoginStartResponse> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "SRP_LOGIN_CHALLENGE_CREATED",
                "Login challenge created successfully",
                result

        );
        return ResponseEntity.ok(response);
    }
    @PostMapping("/srp/finish")
    public ResponseEntity<ApiResponse<SrpLoginFinishResponse>> finishSrpLogin(
            @Valid @RequestBody SrpLoginFinishRequest request
    ){
        SrpLoginFinishResponse result = srpAuthenticationService.finish(request);
        ApiResponse<SrpLoginFinishResponse> response =
                ApiResponse.success(HttpStatus.OK.value(), "LOGIN_SUCCESSFUL", "Login completed successfully", result);
        return ResponseEntity.ok(response);
    }

}
