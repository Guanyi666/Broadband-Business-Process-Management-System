package com.bbpms.dispatch.dto;

import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DispatchQueryReq extends BaseDTO {
    private Long workOrderId;
    private Long installerId;
    /** AUTO / MANUAL / REASSIGN. */
    private String strategy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}