package com.datingapp.auth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    private final Map<String, Bucket> refreshTokenBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> deletionBuckets = new ConcurrentHashMap<>();

    @Bean
    public Map<String, Bucket> refreshTokenBuckets() {
        return refreshTokenBuckets;
    }

    @Bean
    public Map<String, Bucket> deletionBuckets() {
        return deletionBuckets;
    }

    public Bucket getRefreshTokenBucket(String userId) {
        return refreshTokenBuckets.computeIfAbsent(userId, k -> createRefreshTokenBucket());
    }

    public Bucket getDeletionBucket(String userId) {
        return deletionBuckets.computeIfAbsent(userId, k -> createDeletionBucket());
    }

    private Bucket createRefreshTokenBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createDeletionBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(24)));
        return Bucket.builder().addLimit(limit).build();
    }
} 