package com.bbpms.install.config;

import com.bbpms.install.bss.BssClient;
import com.bbpms.install.bss.MockBssClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback BSS client: if no other {@link BssClient} bean is on the
 * classpath, register a default {@link MockBssClient} so the install
 * pipeline always has somewhere to call.
 */
@Slf4j
@Configuration
public class BssConfig {

    @Bean
    @ConditionalOnMissingBean(BssClient.class)
    public BssClient defaultBssClient(InstallProperties props) {
        log.warn("No BssClient bean found — registering MockBssClient as fallback.");
        return new MockBssClient(props);
    }
}