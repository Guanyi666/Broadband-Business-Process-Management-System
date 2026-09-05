package com.bbpms.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "请假申请请求")
public class LeaveApplyReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请假类型白名单：CASUAL 事假 / ANNUAL 年假 / SICK 病假 / COMPASSIONATE 丧假 / UNPAID 无薪假 */
    public static final String TYPE_REGEX = "CASUAL|ANNUAL|SICK|COMPASSIONATE|UNPAID";

    @NotBlank(message = "请假类型不能为空")
    @Pattern(regexp = TYPE_REGEX, message = "请假类型不合法（仅支持事假/年假/病假/丧假/无薪假）")
    @Schema(description = "CASUAL | ANNUAL | SICK | COMPASSIONATE | UNPAID")
    private String leaveType;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间不能早于当前时间")
    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间不能早于当前时间")
    @Schema(description = "结束时间")
    private LocalDateTime endAt;

    @NotBlank(message = "请假原因不能为空")
    @Size(min = 2, max = 500, message = "请假原因需在 2~500 个字符之间")
    @Schema(description = "请假原因")
    private String reason;

    @Size(max = 500, message = "附件 URL 不能超过 500 个字符")
    @Schema(description = "附件 URL（SICK 通常必填）")
    private String attachmentUrl;
}
