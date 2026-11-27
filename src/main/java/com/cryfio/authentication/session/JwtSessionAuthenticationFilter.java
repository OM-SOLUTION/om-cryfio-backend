package com.cryfio.authentication.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtSessionAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder jwtDecoder;
    private final SessionRegistryService sessionRegistryService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "PUT".equals(request.getMethod()) && "/api/v1/registrations/encryption-material".equals(request.getServletPath());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException , IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if(token.isBlank()){
            sendUnauthorized(response);
            return;
        }
        try{
           Jwt jwt = jwtDecoder.decode(token);
            UUID accountId = UUID.fromString(jwt.getSubject());
            String sessionIdClaim = jwt.getClaimAsString("sid");
            if(sessionIdClaim == null){
                sendUnauthorized(response);
                return;
            }
            UUID sessionId = UUID.fromString(sessionIdClaim);
            if(!sessionRegistryService.isActive(sessionId,accountId)){
                sendUnauthorized(response);
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(new AuthenticatedSession(accountId,sessionId), null, List.of());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            try{
                filterChain.doFilter(request, response);
            }finally {
                SecurityContextHolder.clearContext();
            }
        }catch (JwtException | IllegalArgumentException exception){
            sendUnauthorized(response);
        }


    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid or revoked access token");
    }
}
