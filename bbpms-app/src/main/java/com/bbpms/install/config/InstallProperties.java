package com.bbpms.install.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalised install / BSS configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.install")
public class InstallProperties {

    private Bss bss = new Bss();
    private Gps gps = new Gps();
    private Photos photos = new Photos();
    private Signal signal = new Signal();

    @Data
    public static class Bss {
        private Boolean mockEnabled = Boolean.TRUE;
        private Double failureRate = 0.05;
        private Long minDelayMs = 200L;
        private Long maxDelayMs = 2000L;
    }

    @Data
    public static class Gps {
        private Integer thresholdMeters = 200;
    }

    @Data
    public static class Photos {
        private Integer minCount = 3;
    }

    @Data
    public static class Signal {
        /** Warn (but allow) when signal &lt;= this dBm threshold. */
        private Integer warnDbm = -27;
    }
}