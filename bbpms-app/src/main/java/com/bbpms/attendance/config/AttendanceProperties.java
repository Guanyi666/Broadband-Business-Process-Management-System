package com.bbpms.attendance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Attendance module config. Bound to {@code bbpms.attendance.*} in
 * application*.yml.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.attendance")
public class AttendanceProperties {

    /** Auto sign-out installers who haven't heartbeated for this many hours. */
    private Integer autoCheckoutHours = 8;

    /** After this minute past 09:00 a clock-in is considered "late". */
    private Integer lateThresholdMinutes = 15;

    /** Clock-out earlier than this local time counts as an early leave in the
     *  monthly summary (SA-P2-001). */
    private String offDutyTime = "17:00";

    /** Attendance records older than this many days are purged nightly. */
    private Integer historyRetentionDays = 90;

    /** Hard cap on a single break; clocking back in is forced after this. */
    private Integer breakMaxMinutes = 120;

    /** Whether to use a separate Redis ZSET for attendance presence
     *  (otherwise installer_profile.on_duty is the only signal). */
    private Boolean useActiveZset = true;
}
