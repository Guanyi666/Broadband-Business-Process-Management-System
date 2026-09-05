package com.bbpms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "手动派单请求")
public class ManualDispatchReq {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "请选择装维人员")
    private Long installerId;

    @Schema(description = "派单员ID（可选，默认为当前用户）")
    private Long dispatcherId;

    @NotBlank(message = "派单原因不能为空")
    @Size(max = 200, message = "派单原因不能超过 200 个字符")
    private String reason;
}