package com.datingapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProfileDeletionConfirmation {
    @NotBlank(message = "Confirmation code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Invalid confirmation code format")
    private String confirmationCode;
} 