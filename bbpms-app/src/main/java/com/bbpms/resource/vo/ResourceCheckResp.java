package com.bbpms.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 资源核查结果。 */
@Data
@Schema(description = "资源核查结果")
public class ResourceCheckResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** RESOURCE_OK | RESOURCE_INSUFFICIENT | NO_COVERAGE */
    @Schema(description = "核查结果")
    private String status;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "小区ID")
    private Long communityId;

    @Schema(description = "小区名称")
    private String communityName;

    @Schema(description = "楼栋ID")
    private Long buildingId;

    @Schema(description = "楼栋名称")
    private String buildingName;

    @Schema(description = "单元ID")
    private Long unitId;

    @Schema(description = "单元名称")
    private String unitName;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "房号")
    private String roomNo;
}