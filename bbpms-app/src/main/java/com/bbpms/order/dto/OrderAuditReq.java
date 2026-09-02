package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit action payload. {@code pass=true} transitions
 * {@code CREATED -> AUDITED}, {@code pass=false} transitions
 * {@code CREATED -> CANCELLED} (rejected).
 */
@Data
@Schema(description = "订单审核请求")
public class OrderAuditReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否审核通过")
    private Boolean pass;

    @Schema(description = "审核备注")
    private String remark;

    @Schema(description = "最终预约上门时间")
    private LocalDateTime appointmentTime;
}
