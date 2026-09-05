package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PUT /api/orders/{id}/appointment request body.
 */
@Data
@Schema(description = "更新预约请求")
public class AppointmentUpsertReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "预约上门时间")
    @Future(message = "预约开始时间不能早于当前时间")
    private LocalDateTime appointmentTime;

    @Schema(description = "联系手机号")
    @Pattern(regexp = OrderCreateReq.PHONE_REGEX, message = "手机号格式不正确")
    private String contactPhone;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
