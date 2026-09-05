package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "装维人员位置上报")
public class InstallerLocationDTO {

    @Schema(description = "装维用户ID")
    @NotNull(message = "装维用户ID不能为空")
    private Long userId;

    @Schema(description = "纬度")
    @DecimalMin(value = "-90.0", message = "纬度范围 -90 ~ 90")
    @DecimalMax(value = "90.0", message = "纬度范围 -90 ~ 90")
    private BigDecimal lat;

    @Schema(description = "经度")
    @DecimalMin(value = "-180.0", message = "经度范围 -180 ~ 180")
    @DecimalMax(value = "180.0", message = "经度范围 -180 ~ 180")
    private BigDecimal lng;

    @Schema(description = "是否在岗：0 否，1 是")
    @Min(value = 0, message = "在岗状态只能为 0 或 1")
    @Max(value = 1, message = "在岗状态只能为 0 或 1")
    private Integer onDuty;
}
