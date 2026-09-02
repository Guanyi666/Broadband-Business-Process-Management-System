package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Cancellation payload. The order is transitioned to
 * {@link com.bbpms.common.enums.OrderStatus#CANCELLED} with the supplied reason.
 */
@Data
@Schema(description = "订单取消请求")
public class OrderCancelReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "取消原因不能为空")
    @Schema(description = "取消原因")
    private String reason;
}
