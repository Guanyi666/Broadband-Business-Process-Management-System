package com.bbpms.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InstallerVO {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private Integer onDuty;

    private BigDecimal lat;

    private BigDecimal lng;

    private Integer workload;

    private BigDecimal rating;

    private LocalDateTime lastActiveAt;
}