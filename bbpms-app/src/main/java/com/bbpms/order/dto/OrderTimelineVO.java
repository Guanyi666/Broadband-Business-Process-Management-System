package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Combined order timeline row. Sourced from {@code order_audit_log} and
 * {@code work_order_timeline} (joined client-side by
 * {@link com.bbpms.order.service.OrderTimelineService}).
 */
@Data
@Schema(description = "订单时间线事件")
public class OrderTimelineVO {

    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    @Schema(description = "事件来源：ORDER_AUDIT / WORKORDER / DISPATCH")
    private String source;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "事件描述")
    private String description;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作人角色")
    private String operatorRole;

    @Schema(description = "变更前状态编码")
    private String fromStatus;

    @Schema(description = "变更前状态名称")
    private String fromStatusDesc;

    @Schema(description = "变更后状态编码")
    private String toStatus;

    @Schema(description = "变更后状态名称")
    private String toStatusDesc;

    @Schema(description = "操作备注或变更原因")
    private String remark;
}
