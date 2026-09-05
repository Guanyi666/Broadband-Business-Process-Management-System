package com.bbpms.customerportal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐资源管理视图对象（后台「资源管理 → 套餐资源」）。
 */
@Data
@Schema(description = "套餐资源视图")
public class PackageAdminVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "套餐编码（如 PKG-100M）")
    private String code;

    @Schema(description = "套餐名称（中文，如 100M 宽带）")
    private String name;

    @Schema(description = "英文名称（如 100M Broadband，可空）")
    private String nameEn;

    @Schema(description = "带宽规格 Mbps")
    private Integer speedMbps;

    @Schema(description = "月租费")
    private BigDecimal monthlyFee;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
