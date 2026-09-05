package com.bbpms.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "签到请求")
public class ClockInReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "GPS 纬度")
    @DecimalMin(value = "-90.0", message = "纬度范围 -90 ~ 90")
    @DecimalMax(value = "90.0", message = "纬度范围 -90 ~ 90")
    private BigDecimal lat;

    @Schema(description = "GPS 经度")
    @DecimalMin(value = "-180.0", message = "经度范围 -180 ~ 180")
    @DecimalMax(value = "180.0", message = "经度范围 -180 ~ 180")
    private BigDecimal lng;

    @Schema(description = "签到来源：MANUAL | GPS | SCHEDULED")
    @Size(max = 20, message = "签到来源不能超过 20 个字符")
    private String source = "MANUAL";

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
