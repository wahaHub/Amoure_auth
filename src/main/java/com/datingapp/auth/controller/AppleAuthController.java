package com.datingapp.auth.controller;

import com.datingapp.auth.dto.AppleLoginRequest;
import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.service.AppleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth/apple")
@RequiredArgsConstructor
public class AppleAuthController {
    private final AppleAuthService appleAuthService;

    @PostMapping
    public ResponseEntity<AuthResponse> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        log.info("Received Apple login request");
        AuthResponse response = appleAuthService.handleAppleLogin(request);
        log.info("Apple login successful");
        return ResponseEntity.ok(response);
    }
} 