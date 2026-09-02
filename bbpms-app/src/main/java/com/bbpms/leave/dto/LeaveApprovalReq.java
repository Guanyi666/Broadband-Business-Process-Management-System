package com.bbpms.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "请假审批请求")
public class LeaveApprovalReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "审批级别不能为空")
    @Schema(description = "1 或 2")
    private Integer approvalLevel;

    @NotBlank(message = "审批动作不能为空")
    @Schema(description = "APPROVE | REJECT")
    private String action;

    @Schema(description = "审批意见")
    private String comment;
}
