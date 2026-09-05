package com.bbpms.track.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单 / 工单履约轨迹统一返回结构：{@code stages + events + summary}。
 * 契约见 {@code docs/TIMELINE_ANALYSIS.md} §7.6。
 */
@Data
@Schema(description = "订单/工单履约轨迹")
public class TrackResultVO {

    @Schema(description = "流程骨架节点（恒返回全部节点）")
    private List<StageVO> stages;

    @Schema(description = "真实操作事件（右轨）")
    private List<TrackEventVO> events;

    @Schema(description = "汇总信息")
    private TrackSummaryVO summary;
}
