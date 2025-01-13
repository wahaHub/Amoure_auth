package com.datingapp.auth.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppleLinkRequest extends AccountLinkRequest {
    private String identityToken;
    private String fullName;
    private String email;
} 