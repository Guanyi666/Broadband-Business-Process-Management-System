package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 房间 VO（含单元/楼栋/小区名）。 */
@Data
public class RoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long unitId;
    private String unitName;
    private String buildingName;
    private String communityName;
    private String roomNo;
    private Integer isInstalled;
}