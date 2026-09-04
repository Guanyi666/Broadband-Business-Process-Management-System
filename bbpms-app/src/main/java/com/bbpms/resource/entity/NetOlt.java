package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** OLT 设备台账。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_olt")
public class NetOlt extends BaseDO {
    private String name;
    private Long regionId;
    private String ip;
    private String vendor;
    private String model;
    private Integer status;
}
