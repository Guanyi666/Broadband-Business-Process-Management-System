package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 小区 VO（含区域名）。 */
@Data
public class CommunityVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long regionId;
    private String regionName;
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String gridCode;
    private Integer sort;
    private Integer status;
}