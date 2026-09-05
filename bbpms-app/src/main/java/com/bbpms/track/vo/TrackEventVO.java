package com.bbpms.track.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 真实操作事件（右轨 Timeline）。仅聚合日志表 / 派单记录中的真实记录，
 * 不基于主干时间字段伪造。
 */
@Data
@Schema(description = "真实操作事件")
public class TrackEventVO {

    @Schema(description = "事件时间（格式 yyyy-MM-dd HH:mm）")
    private String time;

    @Schema(description = "事件标题")
    private String title;

    @Schema(description = "状态流转描述，如 待审核 → 已审核")
    private String desc;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作人角色")
    private String operatorRole;

    /** ORDER_AUDIT | WORKORDER | DISPATCH */
    @Schema(description = "事件来源 ORDER_AUDIT|WORKORDER|DISPATCH")
    private String source;

    @JsonProperty("isAuto")
    @Schema(description = "是否系统自动操作")
    private Boolean auto;

    @Schema(description = "备注")
    private String remark;
}
