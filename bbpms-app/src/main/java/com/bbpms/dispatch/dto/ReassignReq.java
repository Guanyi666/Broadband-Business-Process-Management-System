package com.bbpms.dispatch.dto;

import lombok.Data;

@Data
public class ReassignReq {
    private Long workOrderId;
    private String reason;
}