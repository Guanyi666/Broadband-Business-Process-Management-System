package com.bbpms.install.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InstallInfoReq {
    private Long workOrderId;
    private String onuMac;
    private String onuSn;
    private String oltPort;
    private BigDecimal signal;
}