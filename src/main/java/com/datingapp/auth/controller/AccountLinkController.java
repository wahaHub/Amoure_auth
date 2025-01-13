package com.datingapp.auth.controller;

import com.datingapp.auth.dto.AppleLinkRequest;
import com.datingapp.auth.dto.PhoneLinkRequest;
import com.datingapp.auth.dto.WeChatLinkRequest;
import com.datingapp.auth.service.AccountLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/link")
@RequiredArgsConstructor
public class AccountLinkController {
    private final AccountLinkService accountLinkService;

    @PostMapping("/wechat")
    public ResponseEntity<Void> linkWeChatAccount(@Valid @RequestBody WeChatLinkRequest request) {
        log.info("Received WeChat account link request");
        accountLinkService.linkWeChatAccount(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/apple")
    public ResponseEntity<Void> linkAppleAccount(@Valid @RequestBody AppleLinkRequest request) {
        log.info("Received Apple account link request");
        accountLinkService.linkAppleAccount(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/send-code")
    public ResponseEntity<Void> sendPhoneLinkVerificationCode(
            @Valid @RequestBody PhoneLinkRequest request) {
        log.info("Sending verification code for phone linking");
        accountLinkService.sendPhoneLinkVerificationCode(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<Void> linkPhoneNumber(@Valid @RequestBody PhoneLinkRequest request) {
        log.info("Received phone number link request");
        accountLinkService.linkPhoneNumber(request);
        return ResponseEntity.ok().build();
    }
} 