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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        log.info("Request path: {}", path);

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
        log.info("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        log.info("Extracted token: {}", token);

        try {
            // Validate token
            if (!jwtService.isTokenValid(token)) {
                log.warn("Token validation failed");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Extract and validate scopes for protected endpoints
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            List<String> scopes = jwtService.extractScopes(token);
            Long userId = jwtService.extractUserId(token);

            // Validate scopes for specific endpoints
            if (path.startsWith("/inventory") && !hasInventoryScope(scopes)) {
                log.warn("🚫 Access denied for path {} - missing inventory scope", path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Create a mutated exchange with the user headers
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Email", username)
                            .header("X-User-Role", role)
                            .header("X-User-Scopes", String.join(",", scopes))
                            .header("X-User-Id", String.valueOf(userId))
                    ).build();


            log.info("✅ Mutated request with headers: X-User-Id={}, X-User-Role={}", userId, role);

            // Pass the mutated exchange to the chain
            return chain.filter(mutatedExchange);


        } catch (Throwable t) {
            log.error("❌ Error during JWT authentication", t);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
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