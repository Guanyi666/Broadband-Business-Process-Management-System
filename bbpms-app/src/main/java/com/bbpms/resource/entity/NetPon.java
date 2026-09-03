package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** PON 板卡/口。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_pon")
public class NetPon extends BaseDO {
    private Long oltId;
    private String name;
    private Integer totalPorts;
    private Integer usedPorts;
}
