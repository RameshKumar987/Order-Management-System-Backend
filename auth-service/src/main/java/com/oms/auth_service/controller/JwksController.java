package com.oms.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class JwksController {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Exposes the public key in JWKS format
     * This is required by OAuth2 resource servers to validate JWT tokens
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        // Create a mock JWKS response with the JWT secret as kid
        // In production, you'd want to use proper JWK format with RSA public key
        Map<String, Object> key = new HashMap<>();
        key.put("kty", "oct");  // Octet sequence (symmetric key)
        key.put("kid", "jwt-secret");
        key.put("use", "sig");
        key.put("alg", "HS512");
        key.put("k", Base64.getEncoder().encodeToString(secret.getBytes()));

        return Map.of("keys", List.of(key));
    }
}

