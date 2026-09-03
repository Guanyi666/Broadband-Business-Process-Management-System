package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 房间（末端可售资源的最小粒度）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_room")
public class NetRoom extends BaseDO {
    private Long unitId;
    private String roomNo;
    /** 0=未安装 1=已安装（在线） */
    private Integer isInstalled;
}
