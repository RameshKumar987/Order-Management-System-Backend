package com.oms.auth_service.service;

import com.oms.auth_service.entity.Role;
import com.oms.auth_service.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:900}")
    private long accessTokenExpiration; // 15 minutes

    public String generateAccessToken(User user) {
        return generateAccessToken(user, new HashMap<>());
    }

    public String generateAccessToken(User user, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>(additionalClaims);
        claims.put("role", user.getRole().getName().name());
        claims.put("scopes", getScopesForRole(user.getRole()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (accessTokenExpiration * 1000)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractScopes(String token) {
        return (java.util.List<String>) Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("scopes");
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private java.util.List<String> getScopesForRole(Role role) {
        return switch (role.getName().name()) {
            case "ADMIN" -> java.util.List.of(
                    "admin:read",
                    "admin:write",
                    "inventory:read",
                    "inventory:write",
                    "user:read",
                    "user:write"
            );
            case "INVENTORY" -> java.util.List.of(
                    "inventory:read",
                    "inventory:write"
            );
            case "CUSTOMER" -> java.util.List.of(
                    "order:read",
                    "order:write",
                    "user:read"
            );
            default -> java.util.List.of();
        };
    }
}

