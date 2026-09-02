package com.bbpms.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Appointment view object.
 */
@Data
@Schema(description = "预约视图")
public class AppointmentVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预约时间")
    private LocalDateTime appointmentTime;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否确认（1=确认，0=待确认）")
    private Integer confirmed;
}
