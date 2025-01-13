package com.datingapp.auth.service;

import com.datingapp.auth.dto.AppleLinkRequest;
import com.datingapp.auth.dto.PhoneLinkRequest;
import com.datingapp.auth.dto.WeChatLinkRequest;
import com.datingapp.auth.exception.AuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLinkService {
    private final KeycloakService keycloakService;
    private final WeChatAuthService weChatAuthService;
    private final AppleAuthService appleAuthService;
    private final PhoneVerificationService phoneVerificationService;

    public void linkWeChatAccount(WeChatLinkRequest request) {
        try {
            String userId = keycloakService.getUserIdFromToken(request.getAccessToken());
            // Validate user exists and is active
            keycloakService.validateAndGetUser(userId);
            
            // Get WeChat user info
            JsonNode weChatToken = weChatAuthService.getWeChatAccessToken(request.getCode());
            String accessToken = weChatToken.get("access_token").asText();
            String openId = weChatToken.get("openid").asText();
            JsonNode weChatUserInfo = weChatAuthService.getWeChatUserInfo(accessToken, openId);
            if (weChatUserInfo == null || !weChatUserInfo.has("openid")) {
                throw new AuthenticationException("Invalid WeChat response");
            }
            
            // Check if WeChat account is already linked
            if (keycloakService.isWeChatLinked(userId, openId)) {
                throw new AuthenticationException("WeChat account is already linked");
            }
            
            keycloakService.linkWeChatAccount(
                userId,
                openId,
                weChatUserInfo.get("nickname").asText(),
                weChatUserInfo.get("headimgurl").asText()
            );
            
            log.info("Successfully linked WeChat account for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to link WeChat account", e);
            if (e instanceof AuthenticationException) {
                throw e;
            }
            throw new AuthenticationException("Failed to link WeChat account: " + e.getMessage());
        }
    }

    public void linkAppleAccount(AppleLinkRequest request) {
        try {
            String userId = keycloakService.getUserIdFromToken(request.getAccessToken());
            keycloakService.validateAndGetUser(userId);
            
            Claims claims = appleAuthService.validateAppleToken(request.getIdentityToken());
            String appleUserId = claims.getSubject();
            
            // Check if Apple ID is already linked
            if (keycloakService.isAppleIdLinked(userId, appleUserId)) {
                throw new AuthenticationException("Apple ID is already linked");
            }
            
            String email = claims.get("email", String.class);
            if (email == null) {
                throw new AuthenticationException("Email is required for Apple ID linking");
            }
            
            keycloakService.linkAppleAccount(userId, appleUserId, email);
            log.info("Successfully linked Apple ID for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to link Apple account", e);
            if (e instanceof AuthenticationException) {
                throw e;
            }
            throw new AuthenticationException("Failed to link Apple account: " + e.getMessage());
        }
    }

    public void sendPhoneLinkVerificationCode(PhoneLinkRequest request) {
        try {
            String userId = keycloakService.getUserIdFromToken(request.getAccessToken());
            keycloakService.validateAndGetUser(userId);
            
            // Check if phone number is already linked
            if (keycloakService.isPhoneNumberLinked(userId, request.getPhoneNumber())) {
                throw new AuthenticationException("Phone number is already linked");
            }
            
            phoneVerificationService.sendVerificationCode(request.getPhoneNumber());
            log.info("Sent verification code for phone linking to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send verification code", e);
            if (e instanceof AuthenticationException) {
                throw e;
            }
            throw new AuthenticationException("Failed to send verification code: " + e.getMessage());
        }
    }

    public void linkPhoneNumber(PhoneLinkRequest request) {
        try {
            String userId = keycloakService.getUserIdFromToken(request.getAccessToken());
            keycloakService.validateAndGetUser(userId);
            
            // Check if phone number is already linked
            if (keycloakService.isPhoneNumberLinked(userId, request.getPhoneNumber())) {
                throw new AuthenticationException("Phone number is already linked");
            }
            
            // Verify the code
            if (!phoneVerificationService.verifyCode(request.getPhoneNumber(), request.getCode())) {
                throw new AuthenticationException("Invalid verification code");
            }
            
            keycloakService.linkPhoneNumber(userId, request.getPhoneNumber());
            log.info("Successfully linked phone number for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to link phone number", e);
            if (e instanceof AuthenticationException) {
                throw e;
            }
            throw new AuthenticationException("Failed to link phone number: " + e.getMessage());
        }
    }
} 