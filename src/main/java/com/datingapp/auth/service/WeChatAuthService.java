package com.datingapp.auth.service;

import com.datingapp.auth.config.WeChatProperties;
import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.dto.WeChatLoginRequest;
import com.datingapp.auth.exception.AuthenticationException;
import com.datingapp.auth.exception.WeChatApiException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatAuthService {
    private final WeChatProperties weChatProperties;
    private final RestTemplate restTemplate;
    private final KeycloakService keycloakService;
    
    private static final String WECHAT_ACCESS_TOKEN_URL = 
        "https://api.weixin.qq.com/sns/oauth2/access_token" +
        "?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    
    private static final String WECHAT_USER_INFO_URL = 
        "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";

    public AuthResponse handleWeChatLogin(WeChatLoginRequest request) {
        validateRequest(request);

        try {
            // 1. Exchange code for WeChat access token
            JsonNode weChatToken = getWeChatAccessToken(request.getCode());
            validateWeChatResponse(weChatToken);

            String accessToken = weChatToken.get("access_token").asText();
            String openId = weChatToken.get("openid").asText();

            // 2. Get WeChat user info
            JsonNode userInfo = getWeChatUserInfo(accessToken, openId);
            validateUserInfoResponse(userInfo);

            // 3. Create or update user in Keycloak
            return keycloakService.authenticateWeChatUser(
                openId,
                userInfo.get("nickname").asText(),
                userInfo.get("headimgurl").asText()
            );
        } catch (RestClientException e) {
            log.error("WeChat API communication error", e);
            throw new WeChatApiException("Failed to communicate with WeChat API", "WECHAT_API_ERROR");
        }
    }

    private void validateRequest(WeChatLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new AuthenticationException("WeChat authorization code is required");
        }
    }

    private void validateWeChatResponse(JsonNode response) {
        if (response.has("errcode") && response.get("errcode").asInt() != 0) {
            String errorCode = response.get("errcode").asText();
            String errorMsg = response.get("errmsg").asText();
            log.error("WeChat API error: {} - {}", errorCode, errorMsg);
            throw new WeChatApiException(errorMsg, errorCode);
        }
    }

    private void validateUserInfoResponse(JsonNode response) {
        if (!response.has("openid") || !response.has("nickname")) {
            throw new WeChatApiException("Invalid user info response from WeChat", "INVALID_USER_INFO");
        }
    }

    private JsonNode getWeChatAccessToken(String code) {
        String url = String.format(WECHAT_ACCESS_TOKEN_URL,
            weChatProperties.getAppId(),
            weChatProperties.getAppSecret(),
            code);

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response == null) {
                throw new WeChatApiException("Null response from WeChat API", "NULL_RESPONSE");
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to get WeChat access token", e);
            throw new WeChatApiException("Failed to get access token from WeChat", "TOKEN_ERROR");
        }
    }

    private JsonNode getWeChatUserInfo(String accessToken, String openId) {
        String url = String.format(WECHAT_USER_INFO_URL, accessToken, openId);

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response == null) {
                throw new WeChatApiException("Null response from WeChat user info API", "NULL_RESPONSE");
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to get WeChat user info", e);
            throw new WeChatApiException("Failed to get user info from WeChat", "USER_INFO_ERROR");
        }
    }
} 