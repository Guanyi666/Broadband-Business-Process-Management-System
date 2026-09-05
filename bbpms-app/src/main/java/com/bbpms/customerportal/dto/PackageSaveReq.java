package com.bbpms.customerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 套餐资源新增 / 编辑请求。
 */
@Data
@Schema(description = "套餐资源新增/编辑请求")
public class PackageSaveReq {

    /** 带宽上限：10,000 Mbps = 10 Gbps */
    public static final int SPEED_MAX = 10_000;

    /** 月租费上限：100,000 元 */
    public static final String FEE_MAX = "100000";

    @Schema(description = "套餐编码（如 PKG-200M）")
    @NotBlank(message = "套餐编码不能为空")
    @Size(max = 64, message = "套餐编码不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]*$", message = "套餐编码需以字母开头，仅支持字母/数字/下划线/连字符")
    private String code;

    @Schema(description = "套餐名称（中文）")
    @NotBlank(message = "套餐名称不能为空")
    @Size(min = 2, max = 50, message = "套餐名称需在 2~50 个字符之间")
    private String name;

    @Schema(description = "英文名称（如 100M Broadband）")
    @Size(max = 128, message = "英文名称不能超过 128 个字符")
    private String nameEn;

    @Schema(description = "带宽规格 Mbps")
    @NotNull(message = "带宽规格不能为空")
    @Min(value = 1, message = "带宽规格需在 1M - 10,000M 之间，请输入有效的整数")
    @Max(value = SPEED_MAX, message = "带宽规格需在 1M - 10,000M 之间，请输入有效的整数")
    private Integer speedMbps;

    @Schema(description = "月租费")
    @NotNull(message = "月租费不能为空")
    @DecimalMin(value = "0.0", message = "月租费不能为负数")
    @DecimalMax(value = FEE_MAX, message = "月租费不能超过 100,000 元")
    @Digits(integer = 6, fraction = 2, message = "月租费最多允许 2 位小数")
    private BigDecimal monthlyFee;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述不能超过 500 个字符")
    private String description;

    @Schema(description = "状态 1启用 0禁用")
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    @Schema(description = "排序")
    @Min(value = 0, message = "排序不能为负数")
    @Max(value = 9999, message = "排序不能超过 9999")
    private Integer sort;
}
