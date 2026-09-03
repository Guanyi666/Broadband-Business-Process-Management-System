package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 楼栋 VO（含小区名）。 */
@Data
public class BuildingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long communityId;
    private String communityName;
    private String name;
    private Integer totalFloors;
    private Integer sort;
    private Integer status;
}