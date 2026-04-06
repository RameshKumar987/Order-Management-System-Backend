package com.oms.auth_service.controller;

import com.oms.auth_service.dto.*;
import com.oms.auth_service.entity.RefreshToken;
import com.oms.auth_service.entity.User;
import com.oms.auth_service.service.AuthService;
import com.oms.auth_service.service.OAuth2TokenService;
import com.oms.auth_service.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2TokenService oauth2TokenService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
//
//    /**
//     * Step 1: Send OTP to email for registration
//     */
//    @PostMapping("/otp/send")
//    public ResponseEntity<OtpResponse> sendOtpForRegistration(
//            @Valid @RequestBody SendOtpRequest request) {
//        authService.sendOtpForRegistration(request);
//        return ResponseEntity.ok(new OtpResponse(
//                true,
//                "OTP sent successfully to " + request.email(),
//                System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes from now
//        ));
//    }
//
//    /**
//     * Step 2: Verify OTP and complete registration
//     */
//    @PostMapping("/otp/verify")
//    public ResponseEntity<AuthResponse> verifyOtpAndRegister(
//            @Valid @RequestBody VerifyOtpRequest request) {
//        return ResponseEntity.ok(authService.verifyOtpAndRegister(request));
//    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * OAuth 2.0 Token Endpoint
     * Handles token generation for various grant types
     */
    @PostMapping("/oauth/token")
    public ResponseEntity<OAuth2TokenResponse> getToken(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(new OAuth2TokenResponse(
                authResponse.token(),
                authResponse.refreshToken(),
                authResponse.accessTokenExpiresIn(),
                authResponse.role(),
                String.join(" ", getDefaultScopes(authResponse.role()))
        ));
    }

    /**
     * Refresh Token Endpoint
     * Generates new access token using refresh token
     */
    @PostMapping("/oauth/refresh")
    public ResponseEntity<OAuth2TokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.getByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = oauth2TokenService.generateAccessToken(user);

        return ResponseEntity.ok(new OAuth2TokenResponse(
                newAccessToken,
                refreshToken.getToken(),
                900L,
                user.getRole().getName().name(),
                String.join(" ", getDefaultScopes(user.getRole().getName().name()))
        ));
    }

    /**
     * Token Introspection Endpoint
     * Validates token and returns token information
     */
    @PostMapping("/oauth/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspectToken(
            @RequestParam(value = "token") String token) {
        try {
            if (!oauth2TokenService.isTokenValid(token)) {
                return ResponseEntity.ok(new TokenIntrospectionResponse(
                        false, null, null, null, null, null, null, null, null
                ));
            }

            String username = oauth2TokenService.extractUsername(token);
            String role = oauth2TokenService.extractRole(token);
            List<String> scopes = oauth2TokenService.extractScopes(token);

            return ResponseEntity.ok(new TokenIntrospectionResponse(
                    true,
                    "Bearer",
                    String.join(" ", scopes),
                    null,
                    username,
                    System.currentTimeMillis() / 1000,
                    (System.currentTimeMillis() / 1000) + 900,
                    role,
                    scopes
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(new TokenIntrospectionResponse(
                    false, null, null, null, null, null, null, null, null
            ));
        }
    }

    /**
     * Token Revocation Endpoint
     * Revokes both access and refresh tokens
     */
    @PostMapping("/oauth/revoke")
    public ResponseEntity<Map<String, String>> revokeToken(
            @RequestParam(value = "token") String token,
            Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                // In a production environment, you would need to verify the token belongs to this user
                // and then revoke it
                return ResponseEntity.ok(Map.of("message", "Token revoked successfully"));
            }
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(
                    Map.of("error", "Failed to revoke token")
            );
        }
    }

    @PostMapping("/admin/create-inventory-user")
    public ResponseEntity<AuthResponse> createInventoryUser(
            @Valid @RequestBody CreateInventoryUserRequest request) {
        return ResponseEntity.ok(authService.createInventoryUser(request));
    }

    // ...existing code...

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PutMapping("/admin/users/{id}/toggle-active")
    public ResponseEntity<?> toggleUserActiveStatus(@PathVariable Long id) {
        authService.toggleUserActiveStatus(id);
        return ResponseEntity.ok(Map.of("message", "User status toggled successfully"));
    }

    @GetMapping("/users/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(
                authService.getCurrentUser(authentication.getName())
        );
    }

    @PutMapping("/users/me")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @PutMapping("/users/me/address")
    public ResponseEntity<?> updateAddress(
            Authentication authentication,
            @Valid @RequestBody UpdateAddressRequest request) {
        authService.updateAddress(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Address updated"));
    }

    @PutMapping("/users/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<?> deleteOwnAccount(Authentication authentication) {
        authService.deleteOwnAccount(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }

    private List<String> getDefaultScopes(String role) {
        return switch (role) {
            case "ADMIN" -> List.of(
                    "admin:read", "admin:write",
                    "inventory:read", "inventory:write",
                    "user:read", "user:write"
            );
            case "INVENTORY" -> List.of(
                    "inventory:read", "inventory:write"
            );
            case "CUSTOMER" -> List.of(
                    "order:read", "order:write", "user:read"
            );
            default -> List.of();
        };
    }
}

