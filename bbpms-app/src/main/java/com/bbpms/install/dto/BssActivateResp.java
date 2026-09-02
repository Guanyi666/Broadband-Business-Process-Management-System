package com.bbpms.install.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BssActivateResp {
    private Boolean success;
    private Long orderId;
    private String message;
    private LocalDateTime activatedAt;
}