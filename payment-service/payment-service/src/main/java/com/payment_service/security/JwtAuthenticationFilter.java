package com.payment_service.security;

/**
 * DEPRECATED: JwtAuthenticationFilter is not needed
 *
 * Payment Service uses OAuth2ResourceServer pattern.
 * JWT validation is handled by Spring Security's oauth2ResourceServer config.
 *
 * See: SecurityConfig.java for correct implementation
 * Reference: Order Service uses the same pattern
 */
