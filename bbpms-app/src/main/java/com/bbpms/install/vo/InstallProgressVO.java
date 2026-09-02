package com.bbpms.install.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InstallProgressVO {
    private Long workOrderId;
    private String status;
    private LocalDateTime lastUpdated;
    /** e.g. "arrive / info / photos / signature / complete". */
    private String currentStep;
    private Integer totalSteps;
}