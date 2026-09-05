package com.bbpms.track.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程骨架节点（左轨 Steps）。
 *
 * <p>契约见 {@code docs/TIMELINE_ANALYSIS.md} §7.6。{@code time} 取自真实时间字段，
 * 为 null 时表示尚未发生（对应 state = PENDING），绝不猜测或伪造。</p>
 */
@Data
@Schema(description = "流程骨架节点")
public class StageVO {

    @Schema(description = "节点编码（对应订单/工单状态）")
    private String code;

    @Schema(description = "节点中文名")
    private String name;

    /** DONE | CURRENT | PENDING | EXCEPTION */
    @Schema(description = "节点状态 DONE|CURRENT|PENDING|EXCEPTION")
    private String state;

    @Schema(description = "真实发生时间，无则 null（格式 yyyy-MM-dd HH:mm）")
    private String time;

    @Schema(description = "操作人姓名（主表人员字段解析，优先 nickname）")
    private String operatorName;

    @Schema(description = "操作人角色（客户/审核人员/系统/调度人员/装维人员/客服人员）")
    private String operatorRole;

    @JsonProperty("isAuto")
    @Schema(description = "是否系统自动操作")
    private Boolean auto;
}
