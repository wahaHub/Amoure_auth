package com.datingapp.auth.exception;

public class WeChatApiException extends RuntimeException {
    private final String errorCode;

    public WeChatApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
} 