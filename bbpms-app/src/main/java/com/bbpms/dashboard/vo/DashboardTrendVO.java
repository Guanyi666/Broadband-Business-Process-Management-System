package com.bbpms.dashboard.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GET /api/dashboard/trend 响应体
 * 三数组严格等长 = days；缺失日已由服务层补 0（绝不返回残缺序列）
 */
@Data
@NoArgsConstructor
public class DashboardTrendVO {

    /** 日期序列（升序 yyyy-MM-dd，含今日） */
    private List<String> dates;

    /** 每日订单创建数（broadband_order.create_time） */
    private List<Long> orderCreatedCounts;

    /** 每日工单完成数（work_order.finish_time） */
    private List<Long> workOrderFinishedCounts;

    public DashboardTrendVO(List<String> dates, List<Long> orderCreatedCounts, List<Long> workOrderFinishedCounts) {
        this.dates = dates;
        this.orderCreatedCounts = orderCreatedCounts;
        this.workOrderFinishedCounts = workOrderFinishedCounts;
    }
}
