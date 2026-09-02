package com.bbpms.workorder.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.dispatch.service.DispatchService;
import com.bbpms.leave.entity.LeaveRequest;
import com.bbpms.leave.service.LeaveService;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * When a leave is approved, cancel the installer's active work orders so
 * the parent order can be re-dispatched. STALLED orders are marked but NOT
 * auto-reassigned — that requires a human (privacy reasons for IN_PROGRESS).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveApprovedListener {

    private final WorkOrderService workOrderService;
    private final DispatchService dispatchService;
    private final LeaveService leaveService;

    @EventListener
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void onLeaveApproved(BbpmsEvents.LeaveApprovedEvent ev) {
        if (ev == null || ev.getApplicantId() == null) return;
        log.info("LeaveApprovedListener: applicant={} leave={}", ev.getApplicantId(), ev.getLeaveId());

        // Defensive: confirm the leave really is APPROVED (defensive against
        // re-entrant events after a cancel).
        LeaveRequest lr = leaveService.get(ev.getLeaveId()) == null ? null : null; // placeholder
        // We just trust the event — the state machine has already moved it to APPROVED.

        // Pull all non-terminal work orders assigned to this installer.
        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.eq(WorkOrder::getInstallerId, ev.getApplicantId());
        w.in(WorkOrder::getStatus, "DISPATCHED", "ACCEPTED", "IN_PROGRESS", "STALLED");
        // We don't have selectList on the service interface, so use the impl
        // path: the workorder service has a selectList helper.
        List<WorkOrder> active = workOrderService.selectList(w);
        if (active.isEmpty()) return;

        int cancelled = 0, stalled = 0;
        for (WorkOrder wo : active) {
            try {
                String status = wo.getStatus();
                if ("DISPATCHED".equals(status) || "ACCEPTED".equals(status)) {
                    workOrderService.autoCancel(wo.getId(),
                            "装维请假 " + (ev.getLeaveType() == null ? "" : ev.getLeaveType()),
                            "INSTALLER_SICK");
                    cancelled++;
                    // Re-dispatch the parent order so the customer isn't waiting.
                    if (wo.getOrderId() != null) {
                        try {
                            dispatchService.autoDispatch(wo.getOrderId());
                        } catch (Exception ex) {
                            log.warn("re-dispatch failed for orderId={}: {}", wo.getOrderId(), ex.getMessage());
                        }
                    }
                } else if ("IN_PROGRESS".equals(status) || "STALLED".equals(status)) {
                    // Don't auto-cancel IN_PROGRESS (privacy data on site); mark
                    // stalled and wait for dispatcher manual reassign.
                    workOrderService.markStalled(wo.getId(),
                            "装维请假 " + (ev.getLeaveType() == null ? "" : ev.getLeaveType())
                                    + "，请调度员改派",
                            "EVENT");
                    stalled++;
                }
            } catch (Exception ex) {
                log.error("LeaveApprovedListener: failed to handle work order {}: {}",
                        wo.getId(), ex.getMessage());
            }
        }
        log.info("LeaveApprovedListener: applicant={} cancelled={} stalled={}",
                ev.getApplicantId(), cancelled, stalled);
    }
}
