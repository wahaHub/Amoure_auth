package com.datingapp.auth.service;

import com.datingapp.auth.config.AppleProperties;
import com.datingapp.auth.dto.AppleLoginRequest;
import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {
    private final AppleProperties appleProperties;
    private final KeycloakService keycloakService;
    private final AppleJwtValidator jwtValidator;

    Claims validateAppleToken(String identityToken) {
        return jwtValidator.validateToken(identityToken);
    }

    public AuthResponse handleAppleLogin(AppleLoginRequest request) {
        try {
            // 1. Validate Apple identity token
            Claims claims = jwtValidator.validateToken(request.getIdentityToken());
            
            // 2. Extract user information
            String appleUserId = claims.getSubject();
            String email = claims.get("email", String.class);
            
            // 3. Create or update user in Keycloak
            return keycloakService.authenticateAppleUser(
                appleUserId,
                email,
                request.getFullName()
            );
            
        } catch (Exception e) {
            log.error("Apple authentication failed", e);
            throw new AuthenticationException("Failed to authenticate with Apple: " + e.getMessage());
        }
    }
} 