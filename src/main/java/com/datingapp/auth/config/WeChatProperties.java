package com.datingapp.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.auth.wechat")
public class WeChatProperties {
    private String appId;
    private String appSecret;
} 