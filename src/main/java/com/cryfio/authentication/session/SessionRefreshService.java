package com.cryfio.authentication.session;

import com.cryfio.account.domain.Account;
import com.cryfio.account.domain.AccountSession;
import com.cryfio.account.domain.enums.AccountStatus;
import com.cryfio.account.repository.AccountSessionRepository;
import com.cryfio.authentication.dto.RefreshTokenRequest;
import com.cryfio.authentication.dto.RefreshTokenResponse;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionRefreshService {
    private final AccountSessionRepository accountSessionRepository;
    private final SessionTokenService sessionTokenService;
    private final SessionRegistryService sessionRegistryService;

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request){
        byte[] refreshTokenHash = sessionTokenService.hashRefreshToken(request.refreshToken());
        AccountSession session = accountSessionRepository
                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(this::invalidRefreshToken);
        Instant now = Instant.now();
        if(!session.isUsable(now) ||
            session.getAccount().getStatus() != AccountStatus.ACTIVE ||
            !sessionRegistryService.isActive(session.getId(),session.getAccount().getId())
        ){
            throw invalidRefreshToken();
        }
        SessionTokenService.RefreshToken nextRefreshToken = sessionTokenService.rotateRefreshToken(session.getExpiresAt());
        session.rotateRefreshToken(nextRefreshToken.hash());
        String accessToken = sessionTokenService.createAccessToken(
                session.getAccount().getId(),
                session.getId(),
                now
        );
        return new RefreshTokenResponse(accessToken,nextRefreshToken.value());
    }
    private ApiException invalidRefreshToken(){
        return new ApiException(ErrorCode.UNAUTHORIZED,"Invalid, expired, or revoked refresh token");
    }
}
