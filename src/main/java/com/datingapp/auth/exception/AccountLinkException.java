package com.datingapp.auth.exception;

public class AccountLinkException extends RuntimeException {
    private final String errorCode;

    public AccountLinkException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
} 