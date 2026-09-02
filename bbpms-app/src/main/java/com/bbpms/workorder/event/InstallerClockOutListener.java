package com.bbpms.workorder.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.event.BbpmsEvents;
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
 * When an installer clocks out, mark any active IN_PROGRESS work order as
 * STALLED so the dispatcher can pick it up and reassign.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallerClockOutListener {

    private final WorkOrderService workOrderService;

    @EventListener
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void onClockOut(BbpmsEvents.InstallerClockOutEvent ev) {
        if (ev == null || ev.getInstallerId() == null) return;
        log.info("InstallerClockOutListener: installer={}", ev.getInstallerId());

        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.eq(WorkOrder::getInstallerId, ev.getInstallerId());
        w.in(WorkOrder::getStatus, "IN_PROGRESS", "ACCEPTED");
        List<WorkOrder> active = workOrderService.selectList(w);
        if (active.isEmpty()) return;

        for (WorkOrder wo : active) {
            try {
                workOrderService.markStalled(wo.getId(),
                        "装维已签出（workMinutes=" + ev.getWorkMinutes() + "）",
                        "EVENT");
            } catch (Exception ex) {
                log.error("InstallerClockOutListener: failed to stall work order {}: {}",
                        wo.getId(), ex.getMessage());
            }
        }
    }
}
