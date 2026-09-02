package com.bbpms.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Use the Spring-managed bean so the interceptor participates in DI/AOP.
        // Only the truly-public endpoints are excluded — /me, /menus and /logout
        // must run through the interceptor so the caller's identity is available.
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/captcha",
                        "/api/auth/public-key",
                        "/error",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**",
                        "/static/**",
                        "/files/public/**"
                );
    }
}
