package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单元。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_unit")
public class NetUnit extends BaseDO {
    private Long buildingId;
    private String name;
    private Integer sort;
    private Integer status;
}
