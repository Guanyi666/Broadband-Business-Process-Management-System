package com.bbpms.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderReassignReq {

    @NotNull(message = "请选择新的装维人员")
    private Long newInstallerId;

    @NotBlank(message = "改派原因不能为空")
    @Size(max = 200, message = "改派原因不能超过 200 个字符")
    private String reason;
}
