package com.bbpms.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "请假申请请求")
public class LeaveApplyReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请假类型不能为空")
    @Schema(description = "CASUAL | ANNUAL | SICK | COMPASSIONATE | UNPAID")
    private String leaveType;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间")
    private LocalDateTime endAt;

    @NotBlank(message = "请假原因不能为空")
    @Schema(description = "请假原因")
    private String reason;

    @Schema(description = "附件 URL（SICK 通常必填）")
    private String attachmentUrl;
}
