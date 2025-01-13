package com.datingapp.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.auth.apple")
public class AppleProperties {
    private String teamId;
    private String keyId;
    private String privateKey;
    private String clientId;
    private String keyPath;
} 