package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 单元 VO（含楼栋/小区名）。 */
@Data
public class UnitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long buildingId;
    private String buildingName;
    private String communityName;
    private String name;
    private Integer sort;
    private Integer status;
}