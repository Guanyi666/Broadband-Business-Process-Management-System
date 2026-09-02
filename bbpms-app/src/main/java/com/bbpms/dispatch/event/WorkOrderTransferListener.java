package com.bbpms.dispatch.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.dispatch.dto.ReassignReq;
import com.bbpms.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to installer-side transfer / dispatch-failed signals and triggers
 * a reassignment run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderTransferListener {

    private final DispatchService dispatchService;

    @Async
    @EventListener
    public void onTransfer(BbpmsEvents.WorkOrderTransferEvent event) {
        handle("transfer", event.getWorkOrderId(), event.getReason());
    }

    @Async
    @EventListener
    public void onDispatchFailed(BbpmsEvents.WorkOrderDispatchFailedEvent event) {
        handle("dispatch-failed", event.getWorkOrderId(), event.getReason());
    }

    private void handle(String tag, Long workOrderId, String reason) {
        if (workOrderId == null) return;
        log.info("WorkOrderTransferListener [{}] workOrderId={}", tag, workOrderId);
        try {
            ReassignReq req = new ReassignReq();
            req.setWorkOrderId(workOrderId);
            req.setReason(reason == null ? tag : reason);
            dispatchService.reassign(req);
        } catch (Exception e) {
            log.error("reassign failed for workOrderId={}: {}", workOrderId, e.getMessage(), e);
        }
    }
}