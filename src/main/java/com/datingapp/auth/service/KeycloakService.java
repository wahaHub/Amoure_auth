package com.datingapp.auth.service;

import com.datingapp.auth.dto.AuthResponse;
import com.datingapp.auth.exception.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
public class KeycloakService {
    private Keycloak keycloak;
    private final String serverUrl;
    private final String clientId;
    private final String clientSecret;
    private final String realm;

    public KeycloakService(
        @Value("${keycloak.auth-server-url}") String serverUrl,
        @Value("${keycloak.resource}") String clientId,
        @Value("${keycloak.credentials.secret}") String clientSecret,
        @Value("${keycloak.realm}") String realm) {
        this.serverUrl = serverUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.realm = realm;
    }

    public AuthResponse authenticateWeChatUser(String openId, String nickname, String avatarUrl) {
        UsersResource usersResource = keycloak.realm(realm).users();
        
        // Check if user exists
        List<UserRepresentation> existingUsers = usersResource
            .searchByUsername(openId, true);
        
        boolean isNewUser = existingUsers.isEmpty();
        
        if (isNewUser) {
            // Create new user
            UserRepresentation user = new UserRepresentation();
            user.setUsername(openId);
            user.setEnabled(true);
            user.setFirstName(nickname);
            user.setAttributes(Collections.singletonMap("avatar_url", 
                Collections.singletonList(avatarUrl)));

            usersResource.create(user);
        }

        // Get tokens
        String[] tokens = getTokens(openId);
        
        return AuthResponse.builder()
            .accessToken(tokens[0])
            .refreshToken(tokens[1])
            .expiresIn(3600)
            .tokenType("Bearer")
            .isNewUser(isNewUser)
            .build();
    }

    public AuthResponse authenticateAppleUser(String appleUserId, String email, String fullName) {
        UsersResource usersResource = keycloak.realm(realm).users();
        
        // Check if user exists
        List<UserRepresentation> existingUsers = usersResource
            .searchByUsername("apple:" + appleUserId, true);
        
        boolean isNewUser = existingUsers.isEmpty();
        
        if (isNewUser) {
            // Create new user
            UserRepresentation user = new UserRepresentation();
            user.setUsername("apple:" + appleUserId);
            user.setEmail(email);
            user.setEnabled(true);
            user.setEmailVerified(true);
            
            if (fullName != null) {
                user.setFirstName(fullName);
            }
            
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("apple_user_id", Collections.singletonList(appleUserId));
            user.setAttributes(attributes);

            usersResource.create(user);
        }

        // Get tokens
        String[] tokens = getTokens("apple:" + appleUserId);
        
        return AuthResponse.builder()
            .accessToken(tokens[0])
            .refreshToken(tokens[1])
            .expiresIn(3600)
            .tokenType("Bearer")
            .isNewUser(isNewUser)
            .build();
    }

    public AuthResponse authenticatePhoneUser(String phoneNumber) {
        UsersResource usersResource = keycloak.realm(realm).users();
        
        // Check if user exists
        List<UserRepresentation> existingUsers = usersResource
            .searchByUsername("phone:" + phoneNumber, true);
        
        boolean isNewUser = existingUsers.isEmpty();
        
        if (isNewUser) {
            // Create new user
            UserRepresentation user = new UserRepresentation();
            user.setUsername("phone:" + phoneNumber);
            user.setEnabled(true);
            
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("phone_number", Collections.singletonList(phoneNumber));
            user.setAttributes(attributes);

            usersResource.create(user);
        }

        // Get tokens
        String[] tokens = getTokens("phone:" + phoneNumber);
        
        return AuthResponse.builder()
            .accessToken(tokens[0])
            .refreshToken(tokens[1])
            .expiresIn(3600)
            .tokenType("Bearer")
            .isNewUser(isNewUser)
            .build();
    }

    private String[] getTokens(String username) {
        try {
            // Create a temporary Keycloak instance for token generation
            Keycloak tokenKeycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(username)
                .grantType(OAuth2Constants.PASSWORD)
                // Use a temporary password since we're using direct access grants
                .password(generateTemporaryPassword())
                .build();

            // Get the token
            AccessTokenResponse response = tokenKeycloak.tokenManager().getAccessToken();
            
            if (response == null || response.getToken() == null) {
                throw new AuthenticationException("Failed to generate tokens");
            }

            // Return both access and refresh tokens
            return new String[]{response.getToken(), response.getRefreshToken()};

        } catch (Exception e) {
            log.error("Failed to generate tokens for user: {}", username, e);
            throw new AuthenticationException("Token generation failed: " + e.getMessage());
        }
    }

    private String generateTemporaryPassword() {
        // Generate a secure random password
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public void linkWeChatAccount(String userId, String weChatOpenId, String nickname, String avatarUrl) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        
        // Add WeChat attributes
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        attributes.put("wechat_open_id", Collections.singletonList(weChatOpenId));
        attributes.put("wechat_nickname", Collections.singletonList(nickname));
        attributes.put("wechat_avatar_url", Collections.singletonList(avatarUrl));
        
        user.setAttributes(attributes);
        userResource.update(user);
    }

    public void linkAppleAccount(String userId, String appleUserId, String email) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        
        // Add Apple attributes
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        attributes.put("apple_user_id", Collections.singletonList(appleUserId));
        if (email != null) {
            user.setEmail(email);
            user.setEmailVerified(true);
        }
        
        user.setAttributes(attributes);
        userResource.update(user);
    }

    public void linkPhoneNumber(String userId, String phoneNumber) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        
        // Add phone attributes
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        attributes.put("phone_number", Collections.singletonList(phoneNumber));
        attributes.put("phone_verified", Collections.singletonList("true"));
        
        user.setAttributes(attributes);
        userResource.update(user);
    }

    public String getUserIdFromToken(String accessToken) {
        try {
            AccessToken token = TokenVerifier.create(accessToken, AccessToken.class).getToken();
            return token.getSubject();
        } catch (Exception e) {
            throw new AuthenticationException("Invalid access token");
        }
    }

    public boolean isWeChatLinked(String userId, String weChatOpenId) {
        // Check if WeChat is already linked to any user
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByAttributes("wechat_open_id="+weChatOpenId);
        
        if (!users.isEmpty()) {
            // If linked to another user, throw exception
            if (!users.get(0).getId().equals(userId)) {
                throw new AuthenticationException("This WeChat account is already linked to another user");
            }
            return true;
        }
        return false;
    }

    public boolean isAppleIdLinked(String userId, String appleUserId) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByAttributes("apple_user_id="+appleUserId);
        
        if (!users.isEmpty()) {
            if (!users.get(0).getId().equals(userId)) {
                throw new AuthenticationException("This Apple ID is already linked to another user");
            }
            return true;
        }
        return false;
    }

    public boolean isPhoneNumberLinked(String userId, String phoneNumber) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByAttributes("phone_number="+phoneNumber);
        
        if (!users.isEmpty()) {
            if (!users.get(0).getId().equals(userId)) {
                throw new AuthenticationException("This phone number is already linked to another user");
            }
            return true;
        }
        return false;
    }

    public UserRepresentation validateAndGetUser(String userId) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            if (!user.isEnabled()) {
                throw new AuthenticationException("User account is disabled");
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to get user details", e);
            throw new AuthenticationException("Invalid user ID or user not found");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            AccessTokenResponse response = refreshUsingHttpCall(
                    serverUrl,
                    realm,
                    clientId,
                    clientSecret,
                    refreshToken
            );

            if (response == null || response.getToken() == null) {
                throw new AuthenticationException("Failed to refresh tokens");
            }

            // 3. Return an AuthResponse with the new tokens
            return AuthResponse.builder()
                .accessToken(response.getToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .tokenType("Bearer")
                .isNewUser(false)
                .build();
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AuthenticationException("Token refresh failed: " + e.getMessage());
        }
    }

    public void logout(String token) {
        try {
            String userId = getUserIdFromToken(token);
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            
            // Revoke all sessions for the user
            userResource.logout();
            log.info("Successfully logged out user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to logout user", e);
            throw new AuthenticationException("Failed to logout: " + e.getMessage());
        }
    }

    public void deleteProfile(String token) {
        try {
            String userId = getUserIdFromToken(token);
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            
            // First, logout all sessions
            userResource.logout();
            
            // Then delete the user
            userResource.remove();
            
            log.info("Successfully deleted user profile: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user profile", e);
            throw new AuthenticationException("Failed to delete profile: " + e.getMessage());
        }
    }

    private AccessTokenResponse refreshUsingHttpCall(String serverUrl,
                                                            String realm,
                                                            String clientId,
                                                            String clientSecret,
                                                            String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(tokenUrl);

            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("grant_type", "refresh_token"));
            formParams.add(new BasicNameValuePair("client_id", clientId));
            formParams.add(new BasicNameValuePair("client_secret", clientSecret));
            formParams.add(new BasicNameValuePair("refresh_token", refreshToken));
            post.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));

            CloseableHttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.getEntity().getContent(), AccessTokenResponse.class);
            } else if (statusCode == 400) {
                throw new AuthenticationException("Invalid refresh token");
            } else if (statusCode == 401) {
                throw new AuthenticationException("Invalid client credentials");
            } else {
                throw new AuthenticationException("Failed to refresh token, HTTP " + statusCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to refresh token: " + e.getMessage(), e);
        }
    }
} 