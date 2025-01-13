package com.datingapp.auth.service;

import com.datingapp.auth.model.ApplePublicKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplePublicKeyCache {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private final Map<String, ApplePublicKey> keyCache = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // Refresh every 24 hours
    public void refreshPublicKeys() {
        try {
            JsonNode response = restTemplate.getForObject(APPLE_KEYS_URL, JsonNode.class);
            if (response != null && response.has("keys")) {
                keyCache.clear();
                JsonNode keys = response.get("keys");
                keys.forEach(key -> {
                    ApplePublicKey publicKey = objectMapper.convertValue(key, ApplePublicKey.class);
                    keyCache.put(publicKey.getKid(), publicKey);
                });
                log.info("Successfully refreshed Apple public keys. Total keys: {}", keyCache.size());
            }
        } catch (Exception e) {
            log.error("Failed to refresh Apple public keys", e);
        }
    }
    
    public ApplePublicKey getPublicKey(String keyId) {
        if (keyCache.isEmpty()) {
            refreshPublicKeys();
        }
        return keyCache.get(keyId);
    }
} 