package com.datingapp.auth.service;

import com.datingapp.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

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
        // Implementation to get tokens from Keycloak
        // This is a simplified version - you'll need to implement the actual token retrieval
        return new String[]{"access_token", "refresh_token"};
    }
} 