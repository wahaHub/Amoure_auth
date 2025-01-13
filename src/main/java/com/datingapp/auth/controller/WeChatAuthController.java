package com.datingapp.auth.controller;

import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.dto.WeChatLoginRequest;
import com.datingapp.auth.service.WeChatAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/wechat")
@RequiredArgsConstructor
public class WeChatAuthController {
    private final WeChatAuthService weChatAuthService;

    @PostMapping
    public ResponseEntity<AuthResponse> weChatLogin(@Valid @RequestBody WeChatLoginRequest request) {
        log.info("Received WeChat login request");
        AuthResponse response = weChatAuthService.handleWeChatLogin(request);
        log.info("WeChat login successful for request");
        return ResponseEntity.ok(response);
    }
} 