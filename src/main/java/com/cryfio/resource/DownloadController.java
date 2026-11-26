package com.cryfio.resource;

import com.cryfio.authentication.session.AuthenticatedSession;
import com.cryfio.common.api.ApiResponse;
import com.cryfio.resource.dto.PrepareDownloadResponse;
import com.cryfio.storage.GarageUploadUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class DownloadController {
    private final DownloadService downloadService;
    @GetMapping("/{resourceId}/download")
    public ResponseEntity<ApiResponse<PrepareDownloadResponse>> prepareDownload(
            @AuthenticationPrincipal AuthenticatedSession curentSession,
            @PathVariable UUID resourceId
    ){
        PrepareDownloadResponse result = downloadService.prepareDownload(curentSession.accountId(),resourceId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "DOWNLOAD_PREPARED",
                        "Download prepared successfully",
                        result
                )
        );
    }
}
