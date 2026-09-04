package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** ONU 终端（安装后绑定房间与 PON 口）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_onu")
public class NetOnu extends BaseDO {
    private Long roomId;
    private Long ponId;
    private String sn;
    private String model;
    /** IN_STOCK | INSTALLED | FAULT | RETIRED */
    private String status;
}
