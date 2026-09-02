package com.bbpms.user.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InstallerLocationDTO {

    private Long userId;

    private BigDecimal lat;

    private BigDecimal lng;

    private Integer onDuty;
}