package com.bbpms.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispatch_rule")
public class DispatchRule extends BaseDO {
    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("weight_distance")
    private Integer weightDistance;

    @TableField("weight_load")
    private Integer weightLoad;

    @TableField("weight_skill")
    private Integer weightSkill;

    @TableField("weight_rating")
    private Integer weightRating;

    @TableField("radius_km")
    private Integer radiusKm;

    @TableField("enabled")
    private Integer enabled;
}