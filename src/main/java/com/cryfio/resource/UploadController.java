package com.cryfio.resource;

import com.cryfio.authentication.session.AuthenticatedSession;
import com.cryfio.common.api.ApiResponse;
import com.cryfio.resource.dto.CompleteUploadRequest;
import com.cryfio.resource.dto.CompleteUploadResponse;
import com.cryfio.resource.dto.InitializeUploadRequest;
import com.cryfio.resource.dto.InitializeUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/uploads")
public class UploadController {
    private final UploadService uploadService;
    @PostMapping
    public ResponseEntity<ApiResponse<InitializeUploadResponse>> initializeUpload(
            @AuthenticationPrincipal AuthenticatedSession currentSession,
            @Valid @RequestBody InitializeUploadRequest request
    ){
        InitializeUploadResponse result = uploadService.initializeUpload(
                currentSession.accountId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(),"UPLOAD_INITIALIZED","Upload initialized successfully",result));
    }
    @PostMapping("/{versionId}/complete")
    public ResponseEntity<ApiResponse<CompleteUploadResponse>> completeUpload(
            @AuthenticationPrincipal AuthenticatedSession currentSession,
            @PathVariable UUID versionId,
            @Valid @RequestBody CompleteUploadRequest request
    ){
        CompleteUploadResponse result = uploadService.completeUpload(
                currentSession.accountId(),
                versionId,
                request
        );
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "UPLOAD_COMPLETED",
                        "Upload completed successfully",
                        result
                )
        );

    }

}
