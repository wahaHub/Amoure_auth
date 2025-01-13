package com.datingapp.auth.controller;

import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.dto.PhoneVerificationRequest;
import com.datingapp.auth.dto.PhoneVerificationCodeRequest;
import com.datingapp.auth.exception.AuthenticationException;
import com.datingapp.auth.service.PhoneVerificationService;
import com.datingapp.auth.service.KeycloakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneAuthController {
    private final PhoneVerificationService phoneVerificationService;
    private final KeycloakService keycloakService;

    @PostMapping("/send-code")
    public ResponseEntity<Void> sendVerificationCode(
            @Valid @RequestBody PhoneVerificationRequest request) {
        log.info("Sending verification code to phone number");
        phoneVerificationService.sendVerificationCode(request.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyCode(
            @Valid @RequestBody PhoneVerificationCodeRequest request) {
        log.info("Verifying code for phone number");
        
        // First verify the code
        boolean isVerified = phoneVerificationService.verifyCode(
            request.getPhoneNumber(), 
            request.getCode()
        );
        
        if (!isVerified) {
            throw new AuthenticationException("Code verification failed");
        }
        
        // If verified, authenticate with Keycloak
        AuthResponse response = keycloakService.authenticatePhoneUser(request.getPhoneNumber());
        
        log.info("Phone verification and authentication successful");
        return ResponseEntity.ok(response);
    }
} 