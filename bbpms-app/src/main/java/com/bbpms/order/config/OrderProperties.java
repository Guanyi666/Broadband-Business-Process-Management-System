package com.bbpms.order.config;

import com.bbpms.common.util.SnowflakeIdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.Serial;
import java.io.Serializable;

/**
 * Order-specific configuration bound from
 * {@code application.yml} key {@code bbpms.order}.
 *
 * <p>Also publishes a singleton {@link SnowflakeIdGenerator} bean that the
 * service uses to mint order numbers of the form {@code BB + snowflake}.</p>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.order")
public class OrderProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * SM4 key used to encrypt customer PII at rest.
     * Must be 32 hex characters (16 bytes) — SM4 only accepts 128-bit keys.
     * Default is derived from SM3 of the old placeholder so that a mis-set
     * key cannot crash PII encryption with an IllegalStateException.
     */
    private String sm4Key = "069735e1bec58883795800cd4c74d402";

    /** Business order-number prefix. */
    private String workorderPrefix = "BB";

    /** Snowflake worker id for this instance. */
    private Long snowflakeWorkerId = 1L;

    /** Snowflake datacenter id for this instance. */
    private Long snowflakeDatacenterId = 1L;

    /** Default work-order timeout (minutes) when auto-dispatching. */
    private Integer defaultWorkorderTimeoutMinutes = 60;

    /** Audit-window hours — orders in CREATED beyond this are flagged. */
    private Integer auditWindowHours = 24;

    @Primary
    @Bean(name = "orderSnowflakeIdGenerator")
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        long w = snowflakeWorkerId == null ? 1L : snowflakeWorkerId;
        long d = snowflakeDatacenterId == null ? 1L : snowflakeDatacenterId;
        log.info("Initialising order SnowflakeIdGenerator worker={} datacenter={}", w, d);
        return new SnowflakeIdGenerator(w, d);
    }
}