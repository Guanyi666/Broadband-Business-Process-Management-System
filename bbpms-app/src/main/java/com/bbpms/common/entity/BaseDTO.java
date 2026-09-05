package com.bbpms.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基类。
 * <p>pageNum/pageSize 设置边界约束，防止恶意超大分页拖垮数据库；
 * 子类继承时需在 Controller 上配合 {@code @Validated} 触发校验。</p>
 */
@Data
public class BaseDTO implements Serializable {

    @Schema(description = "页码，从 1 开始")
    @Min(value = 1, message = "页码必须为正整数")
    @Max(value = 100_000, message = "页码过大")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    @Min(value = 1, message = "每页条数必须为正整数")
    @Max(value = 200, message = "每页条数不能超过 200")
    private Long pageSize = 20L;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "关键字")
    private String keyword;
}
