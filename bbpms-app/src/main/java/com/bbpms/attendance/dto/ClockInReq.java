package com.bbpms.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    private BigDecimal lat;

    @Schema(description = "GPS 经度")
    private BigDecimal lng;

    @Schema(description = "签到来源：MANUAL | GPS | SCHEDULED")
    private String source = "MANUAL";

    @Schema(description = "备注")
    private String remark;
}
