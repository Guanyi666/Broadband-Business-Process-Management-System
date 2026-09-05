package com.bbpms.customerportal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 客户 H5「履约进度」聚合视图。
 *
 * <p>5 节点客户视角骨架：已创建 → 已审核 → 已派单 → 装维接单/施工 → 已完成。
 * 时间严格取订单/工单真实字段，不伪造。{@code canUrge}/{@code urgeCoolDownSeconds}
 * 供前端展示催单按钮与防刷倒计时。</p>
 */
@Data
@Schema(description = "客户履约进度视图")
public class CustomerTrackVO {

    @Data
    @Schema(description = "进度节点")
    public static class Stage {
        @Schema(description = "节点编码")
        private String code;
        @Schema(description = "节点中文名")
        private String name;
        /** DONE | CURRENT | PENDING | EXCEPTION | SKIP */
        @Schema(description = "节点状态 DONE|CURRENT|PENDING|EXCEPTION|SKIP")
        private String state;
        @Schema(description = "真实发生时间，无则 null（yyyy-MM-dd HH:mm）")
        private String time;
        @Schema(description = "节点备注（SKIP 说明 / EXCEPTION 原因）")
        private String remark;
    }

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态码")
    private String status;

    @Schema(description = "订单状态中文")
    private String statusLabel;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "进度节点列表")
    private List<Stage> stages;

    @Schema(description = "进度文案，如 3/5")
    private String progress;

    @Schema(description = "当前是否可催单（状态停留超门槛）")
    private boolean canUrge;

    @Schema(description = "催单冷却剩余秒数（>0 表示冷却中）")
    private long urgeCoolDownSeconds;

    @Schema(description = "催单门槛（分钟），用于前端展示")
    private int urgeThresholdMinutes;

    @Schema(description = "是否终态（已完成/已取消）")
    private boolean terminal;
}
