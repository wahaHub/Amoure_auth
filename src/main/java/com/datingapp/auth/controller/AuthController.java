package com.datingapp.auth.controller;

import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.dto.RefreshTokenRequest;
import com.datingapp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refreshing access token");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        log.info("Token refresh successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("Processing logout request");
        authService.logout(token.replace("Bearer ", ""));
        log.info("Logout successful");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/profile/delete-request")
    public ResponseEntity<Void> requestProfileDeletion(@RequestHeader("Authorization") String token) {
        log.info("Processing profile deletion request");
        authService.requestProfileDeletion(token.replace("Bearer ", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/profile/delete-confirm")
    public ResponseEntity<Void> confirmProfileDeletion(
            @RequestHeader("Authorization") String token,
            @RequestParam String confirmationCode) {
        log.info("Processing profile deletion confirmation");
        authService.confirmProfileDeletion(token.replace("Bearer ", ""), confirmationCode);
        return ResponseEntity.ok().build();
    }
} 