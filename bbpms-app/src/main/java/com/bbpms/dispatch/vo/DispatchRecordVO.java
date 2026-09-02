package com.bbpms.dispatch.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DispatchRecordVO {
    private Long id;
    private Long workOrderId;
    private Long installerId;
    private String strategy;
    private BigDecimal score;
    private String candidatesJson;
    private String reason;
    private LocalDateTime createTime;
}