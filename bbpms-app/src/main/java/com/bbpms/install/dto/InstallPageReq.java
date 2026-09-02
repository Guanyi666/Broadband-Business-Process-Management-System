package com.bbpms.install.dto;

import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstallPageReq extends BaseDTO {
    private Long workOrderId;
    private Long installerId;
    /** PENDING / IN_PROGRESS / COMPLETED / FAILED. */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}