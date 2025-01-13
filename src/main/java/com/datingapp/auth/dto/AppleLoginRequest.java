package com.datingapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppleLoginRequest {
    @NotBlank(message = "Identity token is required")
    private String identityToken;
    
    private String authorizationCode;
    private String fullName;
    private String email;
} 