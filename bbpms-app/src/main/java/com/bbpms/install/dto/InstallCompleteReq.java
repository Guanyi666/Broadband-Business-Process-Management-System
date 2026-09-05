package com.bbpms.install.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "安装完成请求")
public class InstallCompleteReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Valid
    private InstallInfoReq info;

    @Valid
    private List<InstallPhotoReq> photos;

    @Valid
    private InstallSignatureReq signature;

    /** 纬度合法范围：-90 ~ 90 */
    @DecimalMin(value = "-90.0", message = "纬度超出合法范围（-90 ~ 90）")
    @DecimalMax(value = "90.0", message = "纬度超出合法范围（-90 ~ 90）")
    private BigDecimal lat;

    /** 经度合法范围：-180 ~ 180 */
    @DecimalMin(value = "-180.0", message = "经度超出合法范围（-180 ~ 180）")
    @DecimalMax(value = "180.0", message = "经度超出合法范围（-180 ~ 180）")
    private BigDecimal lng;

    /** 施工距离（米），上限 100 km */
    @DecimalMin(value = "0.0", message = "施工距离不能为负数")
    @DecimalMax(value = "100000", message = "施工距离超出合理范围（最大 100 km）")
    private BigDecimal distance;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}