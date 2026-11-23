package com.cryfio.authentication.session;

import com.cryfio.account.domain.AccountSession;
import com.cryfio.account.repository.AccountSessionRepository;
import com.cryfio.authentication.dto.ActiveSessionResponse;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor()
public class AccountSessionService {
    private final AccountSessionRepository accountSessionRepository;
    private final SessionRegistryService sessionRegistryService;

    @Transactional(readOnly = true)
    public List<ActiveSessionResponse> listActiveSessions(UUID accountId,UUID currentSessionId) {
        return accountSessionRepository.findAllByAccount_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActivityAtDesc(accountId, Instant.now())
                .stream()
                .map(session -> new ActiveSessionResponse(
                        session.getId(),
                        session.getDeviceName(),
                        session.getCreatedAt(),
                        session.getLastActivityAt(),
                        session.getExpiresAt(),
                        session.getId().equals(currentSessionId)
                )).toList();
    }
    @Transactional
    public void revokeSession(UUID accountId, UUID sessionId) {
        AccountSession session = accountSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Session was not found"));
        session.revoke(Instant.now());
        sessionRegistryService.revoke(sessionId);
    }
}
