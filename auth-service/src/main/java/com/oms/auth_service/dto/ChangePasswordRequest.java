package com.oms.auth_service.dto;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
