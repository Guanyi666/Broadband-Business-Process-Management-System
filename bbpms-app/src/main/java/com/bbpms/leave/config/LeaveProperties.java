package com.bbpms.leave.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bbpms.leave")
public class LeaveProperties {

    /** Default annual leave quota per installer (hours). */
    private Integer annualDefaultHours = 80;

    /** Max hours that can be carried over year-to-year. */
    private Integer maxCarryOverHours = 40;

    /** If true, SICK leave applications must include an attachment. */
    private Boolean sickNeedsAttachment = true;

    /** Hours threshold above which a leave is auto-escalated to 2 levels. */
    private Double escalationThresholdHours = 16.0;

    /** Min notice hours required to apply (validation only). */
    private Integer minNoticeHours = 4;
}
