package com.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret:1234567890123456789012345678901234567890}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/inventory/products/**", "/inventory/categories/**").permitAll()
                        .pathMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/oauth/token",
                                "/auth/oauth/refresh",
                                "/auth/oauth/introspect",
                                "/.well-known/jwks.json"
                        ).permitAll()
                        .pathMatchers("/auth/admin/**").hasRole("ADMIN")
                        .pathMatchers("/inventory/**").authenticated() // Allow any authenticated user
                        .pathMatchers("/orders/**").authenticated() // Allow any authenticated user
                        .pathMatchers("/payments/**").authenticated() // Allow any authenticated user
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter converter = new org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, reactor.core.publisher.Flux<org.springframework.security.core.GrantedAuthority>>() {
            @Override
            public reactor.core.publisher.Flux<org.springframework.security.core.GrantedAuthority> convert(org.springframework.security.oauth2.jwt.Jwt jwt) {
                String role = jwt.getClaimAsString("role");
                if (role != null && !role.isEmpty()) {
                    return reactor.core.publisher.Flux.just(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
                }
                return reactor.core.publisher.Flux.empty();
            }
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow frontend origins on port 5173 (Vite default) and 3000
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://enterpriseoms.in",
                "https://www.enterpriseoms.in"
        ));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // Auth service signs with JJWT Keys.hmacShaKeyFor() which uses HS256 for secrets < 512 bits.
        // We use MacAlgorithm.HS256 here to match that signing algorithm.
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, 0, secretBytes.length, "HmacSHA256");

        return NimbusReactiveJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
    }
}

