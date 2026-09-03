package com.bbpms.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个 KPI 指标项（GET /api/dashboard/overview → kpis[]）
 * 口径约定（诚实设计，不伪造对比基准）：
 *  - 存量指标（待审核/进行中/停滞）prevValue 与 deltaRate 恒为 null
 *  - 流量指标（今日新增/今日完成）prevValue 为昨日同期值；昨日为 0 时 deltaRate=null
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiItemVO {

    /** 指标键：todayOrders / todayFinishedOrders / pendingAuditOrders / runningWorkorders / todayFinishedWorkorders / stalledWorkorders */
    private String key;

    /** 当前值；null 表示不可用（前端显示 '-'，绝不伪造 0） */
    private Long value;

    /** 昨日同期值（存量指标恒为 null） */
    private Long prevValue;

    /** 环比变化率（%）；无对比基准时为 null */
    private Double deltaRate;

    /** 展示单位：订单 KPI 用「单」，工单 KPI 用「个」 */
    private String unit;
}
