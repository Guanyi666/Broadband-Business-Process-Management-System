package com.bbpms.track.controller;

import com.bbpms.common.result.R;
import com.bbpms.track.service.TrackService;
import com.bbpms.track.vo.TrackResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 / 工单履约双轨时间线轨迹接口。
 *
 * <p>新增端点，不影响现有 {@code GET /api/orders/{id}}（timeline 字段）、
 * {@code GET /api/orders/{id}/timeline}、{@code GET /api/work-orders/timeline/{workOrderId}}。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "履约轨迹", description = "订单与工单双轨时间线")
public class TrackController {

    private final TrackService trackService;

    @Operation(summary = "订单履约轨迹（8 节点骨架 + 事件 + 汇总）")
    @GetMapping("/api/orders/{id}/track")
    @PreAuthorize("hasAuthority('order:view') and !hasRole('CUSTOMER')")
    public R<TrackResultVO> orderTrack(@PathVariable("id") Long id) {
        return R.ok(trackService.getOrderTrack(id));
    }

    @Operation(summary = "工单履约轨迹（5 节点骨架 + 事件 + 汇总）")
    @GetMapping("/api/work-orders/{id}/track")
    @PreAuthorize("hasAuthority('workorder:view')")
    public R<TrackResultVO> workOrderTrack(@PathVariable("id") Long id) {
        return R.ok(trackService.getWorkOrderTrack(id));
    }
}
