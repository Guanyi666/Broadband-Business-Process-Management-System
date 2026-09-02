package com.bbpms.dispatch.dto;

import lombok.Data;

@Data
public class ManualDispatchReq {
    private Long orderId;
    private Long installerId;
    private Long dispatcherId;
    private String reason;
}