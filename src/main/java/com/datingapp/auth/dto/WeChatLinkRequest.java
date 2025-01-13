package com.datingapp.auth.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WeChatLinkRequest extends AccountLinkRequest {
    private String code;
    private String state;
} 