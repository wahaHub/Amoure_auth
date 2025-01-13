package com.datingapp.auth.service;

import com.datingapp.auth.config.RateLimitConfig;
import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.exception.AuthenticationException;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakService keycloakService;
    private final RateLimitConfig rateLimitConfig;
    private final StringRedisTemplate redisTemplate;
    
    private static final String DELETION_CONFIRMATION_PREFIX = "deletion_confirmation:";
    private static final int DELETION_CONFIRMATION_EXPIRY = 5; // minutes

    public AuthResponse refreshToken(String refreshToken) {
        try {
            String userId = keycloakService.getUserIdFromToken(refreshToken);
            Bucket bucket = rateLimitConfig.getRefreshTokenBucket(userId);
            
            if (!bucket.tryConsume(1)) {
                throw new AuthenticationException("Too many refresh token requests. Please try again later.");
            }
            
            return keycloakService.refreshToken(refreshToken);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AuthenticationException("Failed to refresh token: " + e.getMessage());
        }
    }

    public void requestProfileDeletion(String token) {
        String userId = keycloakService.getUserIdFromToken(token);
        Bucket bucket = rateLimitConfig.getDeletionBucket(userId);
        
        if (!bucket.tryConsume(1)) {
            throw new AuthenticationException("Too many deletion attempts. Please try again later.");
        }
        
        // Generate confirmation code
        String confirmationCode = generateConfirmationCode();
        String key = DELETION_CONFIRMATION_PREFIX + userId;
        
        // Store confirmation code with expiry
        redisTemplate.opsForValue().set(key, confirmationCode, DELETION_CONFIRMATION_EXPIRY, TimeUnit.MINUTES);
        
        // TODO: Send confirmation code to user's email or phone
        log.info("Profile deletion requested for user: {}. Confirmation code: {}", userId, confirmationCode);
    }

    public void logout(String token) {
        try {
            keycloakService.logout(token);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new AuthenticationException("Failed to logout: " + e.getMessage());
        }
    }

    public void confirmProfileDeletion(String token, String confirmationCode) {
        try {
            String userId = keycloakService.getUserIdFromToken(token);
            String key = DELETION_CONFIRMATION_PREFIX + userId;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                throw new AuthenticationException("Deletion confirmation expired or not requested");
            }
            
            if (!storedCode.equals(confirmationCode)) {
                throw new AuthenticationException("Invalid confirmation code");
            }
            
            // Perform additional security checks
            validateProfileDeletion(userId);
            
            // Delete the user profile
            keycloakService.deleteProfile(token);
            
            // Clean up confirmation code
            redisTemplate.delete(key);
            
            log.info("Profile successfully deleted for user: {}", userId);
        } catch (Exception e) {
            log.error("Profile deletion failed", e);
            throw new AuthenticationException("Failed to delete profile: " + e.getMessage());
        }
    }

    private void validateProfileDeletion(String userId) {
        UserRepresentation user = keycloakService.validateAndGetUser(userId);
        
        // Check if user has recent suspicious activities
        if (hasRecentSuspiciousActivity(userId)) {
            throw new AuthenticationException("Cannot delete profile due to recent suspicious activity");
        }
        
        // Check if user has pending transactions or obligations
        if (hasPendingObligations(userId)) {
            throw new AuthenticationException("Cannot delete profile with pending obligations");
        }
        
        // Additional security checks as needed
    }

    private boolean hasRecentSuspiciousActivity(String userId) {
        // Implement suspicious activity check
        // This could include:
        // - Recent failed login attempts
        // - Multiple device logins
        // - Unusual activity patterns
        return false; // Placeholder
    }

    private boolean hasPendingObligations(String userId) {
        // Implement pending obligations check
        // This could include:
        // - Unpaid services
        // - Active subscriptions
        // - Ongoing matches or conversations
        return false; // Placeholder
    }

    private String generateConfirmationCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
} 