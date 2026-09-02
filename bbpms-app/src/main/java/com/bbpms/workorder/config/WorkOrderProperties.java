package com.bbpms.workorder.config;

import com.bbpms.common.util.SnowflakeIdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serial;
import java.io.Serializable;

/**
 * Work-order specific configuration loaded from
 * {@code application.yml} key {@code bbpms.workorder}. Also publishes the
 * {@link SnowflakeIdGenerator} bean used to mint work numbers of the form
 * {@code WO + snowflake}.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.workorder")
public class WorkOrderProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** SM4 key for workorder-side encryption (typically same as order module). */
    private String sm4Key = "bbpms-app-sm4-key-CHANGE-ME";

    /** SLA (minutes) before a DISPATCHED work order is considered expired. */
    private Integer acceptTimeoutMinutes = 30;

    /** SLA (hours) before an in-progress work order must complete. */
    private Integer completeTimeoutHours = 24;

    /** Snowflake worker id for this instance. */
    private Long snowflakeWorkerId = 1L;

    /** Snowflake datacenter id for this instance. */
    private Long snowflakeDatacenterId = 1L;

    /** Use mocked dispatch scoring until a real scoring engine ships. */
    private Boolean mockDispatchEnabled = Boolean.TRUE;

    @Bean(name = "workorderSnowflakeIdGenerator")
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        long w = snowflakeWorkerId == null ? 1L : snowflakeWorkerId;
        long d = snowflakeDatacenterId == null ? 1L : snowflakeDatacenterId;
        return new SnowflakeIdGenerator(w, d);
    }
}