package com.oms.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email
) {}

