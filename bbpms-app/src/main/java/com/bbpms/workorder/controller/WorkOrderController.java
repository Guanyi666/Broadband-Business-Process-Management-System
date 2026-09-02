package com.bbpms.workorder.controller;

import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.workorder.dto.WorkOrderCreateReq;
import com.bbpms.workorder.dto.WorkOrderQueryReq;
import com.bbpms.workorder.dto.WorkOrderTransferReq;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.service.WorkOrderService;
import com.bbpms.workorder.vo.WorkOrderDetailVO;
import com.bbpms.workorder.vo.WorkOrderVO;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Work-order REST API. Authentication is enforced by the gateway (JWT);
 * the {@code X-User-Id} header is surfaced by {@code SecurityContextHolder}.
 * Authorization on writes is via {@code @PreAuthorize} based on the
 * {@code workorder:*} permission codes.
 */
@Slf4j
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "工单管理")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "新建工单")
    @PreAuthorize("hasAuthority('workorder:create')")
    @PostMapping
    public R<WorkOrderVO> create(@Valid @RequestBody WorkOrderCreateReq req) {
        return R.ok(workOrderService.create(req));
    }

    @Operation(summary = "工单详情（含时间线 + 订单快照）")
    @GetMapping("/{id}")
    public R<WorkOrderDetailVO> getDetail(@PathVariable("id") Long id) {
        return R.ok(workOrderService.getDetail(id));
    }

    @Operation(summary = "分页查询工单")
    @GetMapping("/page")
    public R<PageResp<WorkOrderVO>> page(WorkOrderQueryReq req) {
        return R.ok(workOrderService.page(req));
    }

    @Operation(summary = "按订单 id 查询工单")
    @GetMapping("/by-order/{orderId}")
    public R<WorkOrderVO> byOrder(@PathVariable("orderId") Long orderId) {
        return R.ok(workOrderService.findByOrderId(orderId));
    }

    @Operation(summary = "当前装维的工单队列")
    @PreAuthorize("hasAuthority('workorder:view-own')")
    @GetMapping("/my")
    public R<PageResp<WorkOrderVO>> my(
            @RequestParam(value = "status", required = false) WorkOrderStatus status,
            @RequestParam(value = "pageNum",  defaultValue = "1")  Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize) {
        Long installerId = SecurityContextHolder.userId();
        WorkOrderQueryReq req = new WorkOrderQueryReq();
        req.setInstallerId(installerId);
        req.setStatus(status);
        req.setPageNum(pageNum);
        req.setPageSize(pageSize);
        return R.ok(workOrderService.page(req));
    }

    @Operation(summary = "接单")
    @PreAuthorize("hasAuthority('workorder:accept')")
    @PostMapping("/{id}/accept")
    public R<WorkOrderVO> accept(@PathVariable("id") Long id) {
        Long uid = SecurityContextHolder.userId();
        return R.ok(workOrderService.accept(id, uid));
    }

    @Operation(summary = "开始施工")
    @PreAuthorize("hasAuthority('workorder:start')")
    @PostMapping("/{id}/start")
    public R<WorkOrderVO> start(@PathVariable("id") Long id) {
        Long uid = SecurityContextHolder.userId();
        return R.ok(workOrderService.start(id, uid));
    }

    @Operation(summary = "完成工单")
    @PreAuthorize("hasAuthority('workorder:complete')")
    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable("id") Long id,
                            @RequestParam(value = "installRecordId", required = false) Long installRecordId) {
        Long uid = SecurityContextHolder.userId();
        workOrderService.complete(id, uid, installRecordId);
        return R.ok();
    }

    @Operation(summary = "转单")
    @PreAuthorize("hasAuthority('workorder:transfer')")
    @PostMapping("/{id}/transfer")
    public R<Void> transfer(@PathVariable("id") Long id,
                            @Valid @RequestBody WorkOrderTransferReq req) {
        Long uid = SecurityContextHolder.userId();
        workOrderService.transfer(id, uid, req.getReason());
        return R.ok();
    }

    @Operation(summary = "取消工单")
    @PreAuthorize("hasAuthority('workorder:cancel')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable("id") Long id,
                          @RequestParam(value = "reason", required = false) String reason) {
        Long uid = SecurityContextHolder.userId();
        workOrderService.cancel(id, uid, reason);
        return R.ok();
    }

    @Operation(summary = "修改工单状态（管理员/调度员）")
    @PreAuthorize("hasAuthority('workorder:update-status')")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable("id") Long id,
                                @RequestParam("status") WorkOrderStatus status,
                                @RequestParam(value = "remark", required = false) String remark) {
        Long uid = SecurityContextHolder.userId();
        workOrderService.updateStatus(id, status, remark, uid);
        return R.ok();
    }

    @Operation(summary = "按 id 查工单单行（管理端原始查询）")
    @GetMapping("/entity/{id}")
    public R<WorkOrder> rawById(@PathVariable("id") Long id) {
        return R.ok(workOrderService.getById(id));
    }

    /* ---------- Phase 5 SLA / lifecycle endpoints ---------- */

    @Operation(summary = "心跳（H5 端每 5 分钟调一次）")
    @PostMapping("/{id}/heartbeat")
    public R<Void> heartbeat(@PathVariable("id") Long id) {
        workOrderService.markHeartbeat(id);
        return R.ok();
    }

    @Operation(summary = "主动上报工单停滞")
    @PreAuthorize("hasAuthority('workorder:report-stall')")
    @PostMapping("/{id}/report-stall")
    public R<Void> reportStall(@PathVariable("id") Long id,
                                @RequestBody Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        workOrderService.markStalled(id, reason, "INSTALLER");
        return R.ok();
    }

    @Operation(summary = "恢复施工（STALLED → IN_PROGRESS）")
    @PreAuthorize("hasAnyAuthority('workorder:resume','workorder:update-status')")
    @PostMapping("/{id}/resume")
    public R<WorkOrderVO> resume(@PathVariable("id") Long id) {
        Long uid = SecurityContextHolder.userId();
        return R.ok(workOrderService.resume(id, uid));
    }

    @Operation(summary = "改派（人工指定新装维）")
    @PreAuthorize("hasAuthority('workorder:reassign')")
    @PostMapping("/{id}/reassign")
    public R<WorkOrderVO> reassign(@PathVariable("id") Long id,
                                    @RequestBody Map<String, Object> body) {
        Long uid = SecurityContextHolder.userId();
        Long newInstallerId = body == null ? null : Long.valueOf(String.valueOf(body.get("newInstallerId")));
        String reason = body == null ? null : String.valueOf(body.get("reason"));
        return R.ok(workOrderService.reassign(id, newInstallerId, uid, reason));
    }

    @Operation(summary = "管理员强制关闭工单")
    @PreAuthorize("hasAuthority('workorder:force-close')")
    @PostMapping("/{id}/force-close")
    public R<Void> forceClose(@PathVariable("id") Long id,
                              @RequestBody Map<String, String> body) {
        Long uid = SecurityContextHolder.userId();
        String reason = body == null ? null : body.get("reason");
        String cancelType = body == null ? null : body.get("cancelType");
        workOrderService.forceClose(id, uid, reason, cancelType);
        return R.ok();
    }

    @Operation(summary = "即将超时的工单列表（用于 SLA 看板）")
    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('workorder:sla:view')")
    public R<List<WorkOrderVO>> expiring(@RequestParam(value = "minutes", defaultValue = "30") int minutes) {
        return R.ok(workOrderService.listExpiring(minutes));
    }
}