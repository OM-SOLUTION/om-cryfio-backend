package com.cryfio.authentication;

import com.cryfio.authentication.dto.ActiveSessionResponse;
import com.cryfio.authentication.session.AccountSessionService;
import com.cryfio.authentication.session.AuthenticatedSession;
import com.cryfio.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account/sessions")
public class AccountSessionController {
    private final AccountSessionService accountSessionService;
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> listSession(
            @AuthenticationPrincipal AuthenticatedSession currentSession
    ){
        List<ActiveSessionResponse> sessions = accountSessionService.listActiveSessions(currentSession.accountId(),currentSession.sessionId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "SESSIONS_RETRIEVED",
                "Active sessions retrieved successfully",
                sessions
        ));
    }
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal AuthenticatedSession currentSession,
            @PathVariable UUID sessionId
    ){
        accountSessionService.revokeSession(currentSession.accountId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "SESSION_REVOKED",
                "Session revoked successfully",
                null
        ));
    }
    @DeleteMapping("/current")
    public ResponseEntity<ApiResponse<Void>> logoutCurrentSession(
            @AuthenticationPrincipal AuthenticatedSession currentSession
    ){
        accountSessionService.revokeSession(currentSession.accountId(), currentSession.sessionId());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "SESSION_REVOKED",
                "Session revoked successfully",
                null
        ));
    }
}
