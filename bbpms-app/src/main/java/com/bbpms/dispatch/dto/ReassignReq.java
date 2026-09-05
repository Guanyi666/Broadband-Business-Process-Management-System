package com.bbpms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "工单改派请求")
public class ReassignReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    @NotBlank(message = "改派原因不能为空")
    @Size(max = 200, message = "改派原因不能超过 200 个字符")
    private String reason;
}