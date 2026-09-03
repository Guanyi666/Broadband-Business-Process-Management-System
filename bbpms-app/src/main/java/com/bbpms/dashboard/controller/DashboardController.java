package com.bbpms.dashboard.controller;

import com.bbpms.common.result.R;
import com.bbpms.dashboard.service.DashboardService;
import com.bbpms.dashboard.vo.DashboardKpiVO;
import com.bbpms.dashboard.vo.EfficiencyBucketVO;
import com.bbpms.dashboard.vo.StatusSliceVO;
import com.bbpms.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:view')")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/kpi")
    public R<DashboardKpiVO> kpi() {
        return R.ok(dashboardService.kpi());
    }

    @GetMapping("/order-trend")
    public R<List<TrendPointVO>> orderTrend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.orderTrend(days));
    }

    @GetMapping("/order-status-dist")
    public R<List<StatusSliceVO>> orderStatusDistribution() {
        return R.ok(dashboardService.orderStatusDistribution());
    }

    @GetMapping("/dispatch-efficiency")
    public R<List<EfficiencyBucketVO>> dispatchEfficiency() {
        return R.ok(dashboardService.dispatchEfficiency());
    }
}
