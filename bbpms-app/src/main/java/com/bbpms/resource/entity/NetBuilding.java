package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 楼栋。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_building")
public class NetBuilding extends BaseDO {
    private Long communityId;
    private String name;
    private Integer totalFloors;
    private Integer sort;
    private Integer status;
}
