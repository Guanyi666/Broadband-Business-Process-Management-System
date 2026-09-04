package com.bbpms.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 小区/园区。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("net_community")
public class NetCommunity extends BaseDO {
    private Long regionId;
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String gridCode;
    private Integer sort;
    private Integer status;
}
