package com.bbpms.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispatch_rule")
public class DispatchRule extends BaseDO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称不能超过 100 个字符")
    @TableField("name")
    private String name;

    @NotNull(message = "距离权重不能为空")
    @Min(value = 0, message = "权重不能为负数")
    @Max(value = 100, message = "权重不能超过 100")
    @TableField("weight_distance")
    private Integer weightDistance;

    @NotNull(message = "负载权重不能为空")
    @Min(value = 0, message = "权重不能为负数")
    @Max(value = 100, message = "权重不能超过 100")
    @TableField("weight_load")
    private Integer weightLoad;

    @NotNull(message = "技能权重不能为空")
    @Min(value = 0, message = "权重不能为负数")
    @Max(value = 100, message = "权重不能超过 100")
    @TableField("weight_skill")
    private Integer weightSkill;

    @NotNull(message = "评分权重不能为空")
    @Min(value = 0, message = "权重不能为负数")
    @Max(value = 100, message = "权重不能超过 100")
    @TableField("weight_rating")
    private Integer weightRating;

    @NotNull(message = "派单半径不能为空")
    @Min(value = 1, message = "派单半径至少 1 公里")
    @Max(value = 500, message = "派单半径不能超过 500 公里")
    @TableField("radius_km")
    private Integer radiusKm;

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    @TableField("enabled")
    private Integer enabled;
}