package com.bbpms.install.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "装维到达请求")
public class InstallArriveReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 纬度合法范围：-90 ~ 90 */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度超出合法范围（-90 ~ 90）")
    @DecimalMax(value = "90.0", message = "纬度超出合法范围（-90 ~ 90）")
    private BigDecimal lat;

    /** 经度合法范围：-180 ~ 180 */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度超出合法范围（-180 ~ 180）")
    @DecimalMax(value = "180.0", message = "经度超出合法范围（-180 ~ 180）")
    private BigDecimal lng;

    @Size(max = 200, message = "地址不能超过 200 个字符")
    private String address;
}