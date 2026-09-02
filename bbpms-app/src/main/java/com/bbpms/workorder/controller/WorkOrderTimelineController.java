package com.bbpms.workorder.controller;

import com.bbpms.common.result.R;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Standalone timeline endpoint — useful when the client wants to poll for
 * incremental updates without re-pulling the full detail payload.
 */
@Slf4j
@RestController
@RequestMapping("/api/work-orders/timeline")
@RequiredArgsConstructor
@Tag(name = "工单时间线")
public class WorkOrderTimelineController {

    private final WorkOrderTimelineService timelineService;

    @Operation(summary = "获取工单时间线（按时间正序）")
    @GetMapping("/{workOrderId}")
    public R<List<WorkOrderTimelineVO>> timeline(@PathVariable("workOrderId") Long workOrderId) {
        return R.ok(timelineService.getTimeline(workOrderId));
    }
}
