package com.bbpms.dispatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalised tuning knobs for the dispatch pipeline.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.dispatch")
public class DispatchProperties {

    /** Default factor weights — must sum to 100. */
    private Integer weightsDistance = 40;
    private Integer weightsLoad     = 25;
    private Integer weightsSkill    = 20;
    private Integer weightsRating   = 15;

    /** Service radius in km. */
    private Integer radiusKm = 30;

    /** Fallback capacity used when the installer's profile has no maxWorkload. */
    private Integer maxLoad = 5;

    /** Redisson try-lock wait/lease (seconds). */
    private Long lockWaitSeconds = 5L;
    private Long lockLeaseSeconds = 30L;

    /** Reassign cooldown (minutes). */
    private Integer reassignCooldownMinutes = 30;
}