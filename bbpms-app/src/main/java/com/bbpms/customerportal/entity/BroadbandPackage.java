package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("broadband_package")
public class BroadbandPackage extends BaseDO {
    private String code;
    private String name;
    /** 英文名称（可选，如 100M Broadband） */
    private String nameEn;
    private Integer speedMbps;
    private BigDecimal monthlyFee;
    private String description;
    private Integer status;
    private Integer sort;
}
