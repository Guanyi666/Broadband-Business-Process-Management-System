package com.bbpms.track.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 轨迹汇总卡数据。契约见 {@code docs/TIMELINE_ANALYSIS.md} §7.6。
 */
@Data
@Schema(description = "轨迹汇总")
public class TrackSummaryVO {

    @Schema(description = "当前状态编码")
    private String currentStatus;

    @Schema(description = "当前状态中文描述")
    private String currentStatusDesc;

    @Schema(description = "当前所处骨架节点索引（1-based）")
    private Integer currentStageIndex;

    @Schema(description = "进度，如 4/8")
    private String progress;

    @Schema(description = "已用时长（创建 → 当前/完成/取消）")
    private String elapsed;

    @Schema(description = "距最近一次活动的等待时长（未终结时）")
    private String waiting;

    @Schema(description = "是否已终结（CLOSED / CANCELLED / 工单终态）")
    private Boolean isTerminal;
}
