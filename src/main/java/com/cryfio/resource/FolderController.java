package com.cryfio.resource;

import com.cryfio.authentication.session.AuthenticatedSession;
import com.cryfio.common.api.ApiResponse;
import com.cryfio.resource.dto.CreateFolderRequest;
import com.cryfio.resource.dto.CreateFolderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {
    private final FolderService folderService;
    @PostMapping
    public ResponseEntity<ApiResponse<CreateFolderResponse>> createFolder(
            @AuthenticationPrincipal AuthenticatedSession currentSession,
            @Valid @RequestBody CreateFolderRequest request
    ){
        CreateFolderResponse result = folderService.createFolder(currentSession.accountId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "FOLDER_CREATED",
                        "Folder created successfully",
                        result
                )
        );
    }
}
