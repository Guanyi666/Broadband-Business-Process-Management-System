package com.bbpms.auth.config;

import com.bbpms.common.util.CryptoUtils;
import com.bbpms.common.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.security.KeyPair;
import java.util.Base64;

@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.jwt")
public class AuthProperties {

    private String rsaPrivateKey;

    private String rsaPublicKey;

    private String refreshSecret;

    private Integer accessTtlMinutes = 30;

    private Integer refreshTtlDays = 7;

    @PostConstruct
    public void initKeys() {
        if (rsaPrivateKey == null || rsaPrivateKey.isEmpty()
                || rsaPublicKey == null || rsaPublicKey.isEmpty()) {
            KeyPair kp = CryptoUtils.rsaGenerate(2048);
            rsaPublicKey = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            rsaPrivateKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        }
        if (refreshSecret == null || refreshSecret.isEmpty()) {
            byte[] bytes = new byte[32];
            new java.security.SecureRandom().nextBytes(bytes);
            refreshSecret = Base64.getEncoder().encodeToString(bytes);
        }
    }

    @Lazy
    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(rsaPrivateKey, rsaPublicKey, refreshSecret);
    }
}