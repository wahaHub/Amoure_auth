package com.datingapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountLinkRequest {
    @NotBlank(message = "Access token is required")
    private String accessToken;  // Current user's access token
} 