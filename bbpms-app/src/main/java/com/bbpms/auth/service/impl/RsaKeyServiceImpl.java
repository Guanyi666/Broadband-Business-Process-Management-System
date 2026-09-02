package com.bbpms.auth.service.impl;

import com.bbpms.auth.config.AuthProperties;
import com.bbpms.auth.service.RsaKeyService;
import com.bbpms.common.util.CryptoUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RsaKeyServiceImpl implements RsaKeyService {

    private final AuthProperties authProperties;

    @PostConstruct
    public void init() {
        if (authProperties.getRsaPrivateKey() == null || authProperties.getRsaPrivateKey().isEmpty()
                || authProperties.getRsaPublicKey() == null || authProperties.getRsaPublicKey().isEmpty()) {
            KeyPair kp = CryptoUtils.rsaGenerate(2048);
            String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String pri = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
            authProperties.setRsaPublicKey(pub);
            authProperties.setRsaPrivateKey(pri);
        }
        if (authProperties.getRefreshSecret() == null || authProperties.getRefreshSecret().isEmpty()) {
            byte[] bytes = new byte[32];
            new java.security.SecureRandom().nextBytes(bytes);
            authProperties.setRefreshSecret(Base64.getEncoder().encodeToString(bytes));
        }
    }

    @Override
    public String getPublicKey() {
        return authProperties.getRsaPublicKey();
    }

    @Override
    public String getPrivateKey() {
        return authProperties.getRsaPrivateKey();
    }
}