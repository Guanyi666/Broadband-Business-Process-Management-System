package com.bbpms.workorder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SLA scheduler config. Bound to {@code bbpms.workorder.sla.*}.
 *
 * <p>Per-business-type overrides are stored in the
 * {@code wo_sla_policy} table (read by the scheduler, not bound here).</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.workorder.sla")
public class WorkOrderSlaProperties {

    /** How often the SLA scheduler scans for timed-out work orders. */
    private Long scanIntervalMs = 30_000L;

    /** DISPATCHED beyond this many minutes without an accept → AUTO_CANCELLED. */
    private Integer acceptTimeoutMinutes = 30;

    /** IN_PROGRESS beyond this many hours without heartbeat → STALLED. */
    private Integer progressHeartbeatTimeoutHours = 4;

    /** STALLED beyond this many hours without resume → AUTO_CANCELLED. */
    private Integer stalledRecoverHours = 24;

    /** Master switch (useful for tests / freeze). */
    private Boolean enabled = true;
}
