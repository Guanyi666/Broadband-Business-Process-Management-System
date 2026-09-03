package com.bbpms.dashboard.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GET /api/dashboard/overview 响应体
 */
@Data
@NoArgsConstructor
public class DashboardOverviewVO {

    /** 数据生成时间（GMT+8, yyyy-MM-dd HH:mm:ss） */
    private String generatedAt;

    /** 统计窗口天数（与请求一致，默认 7） */
    private Integer days;

    /** 6 个 KPI（含存量与流量，见 KpiItemVO 口径） */
    private List<KpiItemVO> kpis;

    /** 订单状态分布（8 态全量，含 0） */
    private List<StatusDistItemVO> orderStatusDist;

    /** 工单状态分布（10 态全量，含 0） */
    private List<StatusDistItemVO> workOrderStatusDist;

    public DashboardOverviewVO(String generatedAt, Integer days, List<KpiItemVO> kpis,
                               List<StatusDistItemVO> orderStatusDist,
                               List<StatusDistItemVO> workOrderStatusDist) {
        this.generatedAt = generatedAt;
        this.days = days;
        this.kpis = kpis;
        this.orderStatusDist = orderStatusDist;
        this.workOrderStatusDist = workOrderStatusDist;
    }
}
