package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 网络区域（地市）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_region")
public class NetRegion extends BaseDO {
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
}
