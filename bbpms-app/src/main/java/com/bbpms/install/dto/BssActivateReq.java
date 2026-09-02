package com.bbpms.install.dto;

import lombok.Data;

@Data
public class BssActivateReq {
    private Long orderId;
    private String customerName;
    private String address;
}