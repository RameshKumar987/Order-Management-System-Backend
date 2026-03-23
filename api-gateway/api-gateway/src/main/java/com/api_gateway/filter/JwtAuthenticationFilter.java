package com.api_gateway.filter;

import com.api_gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // auth APIs and OAuth endpoints - permitAll
        if (path.startsWith("/auth/login") ||
            path.startsWith("/auth/register") ||
            path.startsWith("/auth/oauth/token") ||
            path.startsWith("/auth/oauth/refresh") ||
            path.startsWith("/auth/oauth/introspect")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtService.isTokenValid(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Extract and validate scopes for protected endpoints
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            List<String> scopes = jwtService.extractScopes(token);

            // Add user info to request headers for downstream services
            exchange.getRequest().mutate()
                    .header("X-User-Email", username)
                    .header("X-User-Role", role)
                    .header("X-User-Scopes", String.join(",", scopes))
                    .build();

            // Validate scopes for specific endpoints
            if (path.startsWith("/inventory") && !hasInventoryScope(scopes)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean hasInventoryScope(List<String> scopes) {
        return scopes != null && (
                scopes.contains("inventory:read") ||
                scopes.contains("inventory:write") ||
                scopes.contains("admin:read") ||
                scopes.contains("admin:write")
        );
    }
}

