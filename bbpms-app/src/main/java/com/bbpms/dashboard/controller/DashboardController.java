package com.bbpms.dashboard.controller;

import com.bbpms.common.result.R;
import com.bbpms.dashboard.service.DashboardService;
import com.bbpms.dashboard.vo.DashboardDispatchVO;
import com.bbpms.dashboard.vo.DashboardOverviewVO;
import com.bbpms.dashboard.vo.DashboardTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 只读统计接口（T02）。
 *
 * 权限：全部为 {@code dashboard:view}（种子数据中 6 个角色已全部授予）。
 * 只读：不写任何业务表；days 由服务层归一化（默认 7，窗口 [1,90]）。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据看板")
public class DashboardController {

    private final DashboardService dashboardService;

    /** 总览：6 个 KPI + 订单/工单状态分布（存量全量口径） */
    @Operation(summary = "看板总览（KPI + 状态分布）")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public R<DashboardOverviewVO> overview(@RequestParam(value = "days", required = false) Integer days) {
        return R.ok(dashboardService.overview(days));
    }

    /** 近 N 日趋势：订单新建 vs 工单完成（三数组等长，缺失日补 0） */
    @Operation(summary = "趋势（订单新建 / 工单完成）")
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public R<DashboardTrendVO> trend(@RequestParam(value = "days", required = false) Integer days) {
        return R.ok(dashboardService.trend(days));
    }

    /** 派单时效分桶（派单 → 接单耗时，固定 5 桶含 0） */
    @Operation(summary = "派单时效分布")
    @GetMapping("/dispatch-duration")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public R<DashboardDispatchVO> dispatchDuration(@RequestParam(value = "days", required = false) Integer days) {
        return R.ok(dashboardService.dispatchDuration(days));
    }
}
