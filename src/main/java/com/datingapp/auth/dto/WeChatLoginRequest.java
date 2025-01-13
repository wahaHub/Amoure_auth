package com.datingapp.auth.dto;

import lombok.Data;

@Data
public class WeChatLoginRequest {
    private String code;
    private String state;
} 