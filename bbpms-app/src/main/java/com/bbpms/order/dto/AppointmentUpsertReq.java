package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private LocalDateTime appointmentTime;

    @Schema(description = "联系手机号")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;
}