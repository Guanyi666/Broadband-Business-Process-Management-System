package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    @Schema(description = "审核备注")
    @Size(max = 500, message = "审核备注不能超过 500 个字符")
    private String remark;

    @Schema(description = "最终预约上门时间")
    @Future(message = "预约上门时间不能早于当前时间")
    private LocalDateTime appointmentTime;
}
