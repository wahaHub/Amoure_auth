package com.datingapp.auth.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.datingapp.auth.config.AlibabaSmsProperties;
import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private final AlibabaSmsProperties smsProperties;
    private final StringRedisTemplate redisTemplate;
    private final KeycloakService keycloakService;
    
    private static final String VERIFICATION_CODE_PREFIX = "phone_verification:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    public void sendVerificationCode(String phoneNumber) {
        // Check rate limiting
        checkRateLimit(phoneNumber);

        // Generate verification code
        String code = generateVerificationCode();

        try {
            // Initialize Alibaba Cloud client
            DefaultProfile profile = DefaultProfile.getProfile(
                "default", 
                smsProperties.getAccessKeyId(),
                smsProperties.getAccessKeySecret()
            );
            IAcsClient client = new DefaultAcsClient(profile);

            // Create SMS request
            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phoneNumber);
            request.setSignName(smsProperties.getSignName());
            request.setTemplateCode(smsProperties.getTemplateCode());
            request.setTemplateParam("{\"code\":\"" + code + "\"}");

            // Send SMS
            SendSmsResponse response = client.getAcsResponse(request);
            if (!"OK".equals(response.getCode())) {
                throw new AuthenticationException("Failed to send verification code: " + response.getMessage());
            }

            // Store code in Redis
            String key = VERIFICATION_CODE_PREFIX + phoneNumber;
            redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            
            log.info("Verification code sent to {}", phoneNumber);

        } catch (Exception e) {
            log.error("Failed to send verification code", e);
            throw new AuthenticationException("Failed to send verification code");
        }
    }

    public boolean verifyCode(String phoneNumber, String code) {
        String key = VERIFICATION_CODE_PREFIX + phoneNumber;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new AuthenticationException("Verification code expired or not found");
        }

        if (!storedCode.equals(code)) {
            incrementFailedAttempts(phoneNumber);
            throw new AuthenticationException("Invalid verification code");
        }

        // Delete the used code
        redisTemplate.delete(key);

        return true;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void checkRateLimit(String phoneNumber) {
        String rateLimitKey = "rate_limit:" + phoneNumber;
        String attemptsKey = "attempts:" + phoneNumber;

        Long attempts = redisTemplate.opsForValue().increment(rateLimitKey);
        if (attempts == 1) {
            redisTemplate.expire(rateLimitKey, 24, TimeUnit.HOURS);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new AuthenticationException("Too many verification attempts. Please try again later.");
        }
    }

    private void incrementFailedAttempts(String phoneNumber) {
        String attemptsKey = "failed_attempts:" + phoneNumber;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, 1, TimeUnit.HOURS);
        }

        if (attempts != null && attempts >= 3) {
            redisTemplate.delete(VERIFICATION_CODE_PREFIX + phoneNumber);
            throw new AuthenticationException("Too many failed attempts. Please request a new code.");
        }
    }
} 