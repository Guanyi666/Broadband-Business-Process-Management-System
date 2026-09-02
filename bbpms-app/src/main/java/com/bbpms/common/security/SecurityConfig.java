package com.bbpms.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Canonical security configuration (single source of truth).
 *
 * <p>Authentication is enforced by {@code JwtAuthInterceptor} (returns {@code R<>}
 * 401 responses for missing / invalid tokens) which also bridges the resolved
 * {@link SecurityUser} into Spring Security's context so that
 * {@code @EnableMethodSecurity} + {@code @PreAuthorize} work on controllers.
 *
 * <p>The filter chain itself is deliberately permissive: CSRF / sessions / basic
 * auth are off, and docs / actuator / login are public. The real gates are the
 * interceptor (authenticated? -> 401) and {@code @PreAuthorize} (authorized? -> 403).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/captcha",
                                "/api/auth/public-key",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/doc.html",
                                "/webjars/**",
                                "/static/**",
                                "/error",
                                "/internal/**",
                                "/files/public/**").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
