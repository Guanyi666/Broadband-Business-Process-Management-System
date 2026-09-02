package com.bbpms.install.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InstallArriveReq {
    private Long workOrderId;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;
}