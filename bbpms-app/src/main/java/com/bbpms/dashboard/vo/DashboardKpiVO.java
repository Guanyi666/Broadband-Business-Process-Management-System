package com.bbpms.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiVO {
    private Long todayOrders;
    private Long todayAuditPending;
    private Long runningWorkorders;
    private Long completedToday;
}
