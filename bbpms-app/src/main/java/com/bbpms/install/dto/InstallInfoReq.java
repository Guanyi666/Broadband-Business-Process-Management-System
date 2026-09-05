package com.bbpms.install.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "装机信息上报请求")
public class InstallInfoReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** ONU MAC 地址格式：AA:BB:CC:DD:EE:FF */
    @Pattern(regexp = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$", message = "ONU MAC 地址格式不正确（如 AA:BB:CC:DD:EE:FF）")
    private String onuMac;

    @Size(max = 64, message = "ONU 序列号不能超过 64 个字符")
    private String onuSn;

    @Size(max = 64, message = "OLT 端口不能超过 64 个字符")
    private String oltPort;

    /** 光信号强度（dBm），合理范围 -40 ~ 10 */
    @DecimalMin(value = "-40.0", message = "光信号强度超出合理范围（-40 ~ 10 dBm）")
    @DecimalMax(value = "10.0", message = "光信号强度超出合理范围（-40 ~ 10 dBm）")
    private BigDecimal signal;
}