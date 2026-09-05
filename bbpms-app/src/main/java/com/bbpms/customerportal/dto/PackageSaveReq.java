package com.bbpms.customerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 套餐资源新增 / 编辑请求。
 */
@Data
@Schema(description = "套餐资源新增/编辑请求")
public class PackageSaveReq {

    @Schema(description = "套餐编码（如 PKG-200M）")
    @NotBlank(message = "套餐编码不能为空")
    @Size(max = 64, message = "套餐编码过长")
    private String code;

    @Schema(description = "套餐名称（中文）")
    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 128, message = "套餐名称过长")
    private String name;

    @Schema(description = "英文名称（如 100M Broadband）")
    @Size(max = 128, message = "英文名称过长")
    private String nameEn;

    @Schema(description = "带宽规格 Mbps")
    @NotNull(message = "带宽规格不能为空")
    @Min(value = 1, message = "带宽规格需大于 0")
    private Integer speedMbps;

    @Schema(description = "月租费")
    @NotNull(message = "月租费不能为空")
    @DecimalMin(value = "0.0", message = "月租费不能为负")
    private BigDecimal monthlyFee;

    @Schema(description = "描述")
    @Size(max = 512, message = "描述过长")
    private String description;

    @Schema(description = "状态 1启用 0禁用")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
