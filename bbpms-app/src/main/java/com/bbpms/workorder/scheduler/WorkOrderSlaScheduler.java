package com.bbpms.workorder.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.dispatch.service.DispatchService;
import com.bbpms.workorder.config.WorkOrderSlaProperties;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA scheduler — the heart of the work-order lifecycle hardening.
 *
 * <p>Three independent scans:
 * <ol>
 *   <li>{@link #scanDispatchTimeout()} — DISPATCHED beyond accept window → auto-cancel + re-dispatch</li>
 *   <li>{@link #scanProgressTimeout()} — IN_PROGRESS beyond heartbeat window → STALLED + alert dispatchers</li>
 *   <li>{@link #scanStalledRecovery()}  — STALLED beyond recovery window → auto-cancel + re-dispatch</li>
 * </ol>
 *
 * Each scan is isolated (a failure on one work order doesn't affect others)
 * and uses default thresholds from {@code bbpms.workorder.sla.*}. Per-business-type
 * overrides live in the {@code wo_sla_policy} table (read once per scan).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderSlaScheduler {

    private final WorkOrderService workOrderService;
    private final DispatchService dispatchService;
    private final ApplicationEventPublisher publisher;
    private final WorkOrderSlaProperties slaProps;

    /* ---------- 1. Accept-timeout: DISPATCHED → not accepted in time ---------- */

    @Scheduled(fixedDelayString = "${bbpms.workorder.sla.scan-interval-ms:30000}",
               initialDelay = 30_000L)
    public void scanDispatchTimeout() {
        if (!Boolean.TRUE.equals(slaProps.getEnabled())) return;
        int minutes = slaProps.getAcceptTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofMinutes(minutes));
        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.eq(WorkOrder::getStatus, "DISPATCHED")
         .lt(WorkOrder::getDispatchTime, cutoff)
         .last("LIMIT 50");
        List<WorkOrder> overdue = workOrderService.selectList(w);
        if (overdue.isEmpty()) return;
        log.info("SLA: {} work orders past dispatch timeout ({}min)", overdue.size(), minutes);
        for (WorkOrder wo : overdue) {
            try {
                workOrderService.autoCancel(wo.getId(), "ACCEPT_TIMEOUT", "SYSTEM");
                publisher.publishEvent(new BbpmsEvents.WorkOrderAutoCancelledEvent(
                        wo.getId(), wo.getInstallerId(), "ACCEPT_TIMEOUT", "SYSTEM"));
                if (wo.getOrderId() != null) {
                    dispatchService.autoDispatch(wo.getOrderId());
                }
            } catch (Exception ex) {
                log.error("SLA auto-cancel failed for workOrderId={}: {}", wo.getId(), ex.getMessage());
            }
        }
    }

    /* ---------- 2. Heartbeat-timeout: IN_PROGRESS → no heartbeat in time ---------- */

    @Scheduled(fixedDelayString = "${bbpms.workorder.sla.scan-interval-ms:30000}",
               initialDelay = 45_000L)
    public void scanProgressTimeout() {
        if (!Boolean.TRUE.equals(slaProps.getEnabled())) return;
        int hours = slaProps.getProgressHeartbeatTimeoutHours();
        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofHours(hours));
        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.eq(WorkOrder::getStatus, "IN_PROGRESS")
         .and(qw -> qw.isNull(WorkOrder::getLastActiveAt).or().lt(WorkOrder::getLastActiveAt, cutoff))
         .last("LIMIT 50");
        List<WorkOrder> stale = workOrderService.selectList(w);
        if (stale.isEmpty()) return;
        log.info("SLA: {} work orders past heartbeat timeout ({}h)", stale.size(), hours);
        for (WorkOrder wo : stale) {
            try {
                workOrderService.markStalled(wo.getId(), "HEARTBEAT_TIMEOUT", "SYSTEM");
                publisher.publishEvent(new BbpmsEvents.WorkOrderStalledEvent(
                        wo.getId(), wo.getInstallerId(), "HEARTBEAT_TIMEOUT"));
                // Alert dispatchers (best-effort). The notify module listens for
                // NotifyEvent and will resolve the template, substitute placeholders,
                // and call the configured SMS sender (Aliyun / mock).
                publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                        "SMS", null, null, null, "WORKORDER_SLA_BREACH_SMS",
                        java.util.Map.of("workNo", wo.getWorkNo() == null ? "" : wo.getWorkNo(),
                                "reason", "施工超时无心跳")));
            } catch (Exception ex) {
                log.error("SLA markStalled failed for workOrderId={}: {}", wo.getId(), ex.getMessage());
            }
        }
    }

    /* ---------- 3. Stalled recovery: STALLED → not resumed in time ---------- */

    @Scheduled(fixedDelayString = "${bbpms.workorder.sla.scan-interval-ms:30000}",
               initialDelay = 60_000L)
    public void scanStalledRecovery() {
        if (!Boolean.TRUE.equals(slaProps.getEnabled())) return;
        int hours = slaProps.getStalledRecoverHours();
        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofHours(hours));
        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.eq(WorkOrder::getStatus, "STALLED")
         .lt(WorkOrder::getUpdateTime, cutoff)
         .last("LIMIT 50");
        List<WorkOrder> stalled = workOrderService.selectList(w);
        if (stalled.isEmpty()) return;
        log.info("SLA: {} STALLED work orders past recovery window ({}h)", stalled.size(), hours);
        for (WorkOrder wo : stalled) {
            try {
                workOrderService.autoCancel(wo.getId(), "STALLED_AUTO_RECOVER", "SYSTEM");
                publisher.publishEvent(new BbpmsEvents.WorkOrderAutoCancelledEvent(
                        wo.getId(), wo.getInstallerId(), "STALLED_AUTO_RECOVER", "SYSTEM"));
                if (wo.getOrderId() != null) {
                    dispatchService.autoDispatch(wo.getOrderId());
                }
            } catch (Exception ex) {
                log.error("SLA stalled-recovery failed for workOrderId={}: {}", wo.getId(), ex.getMessage());
            }
        }
    }

    /* ---------- thresholds come from WorkOrderSlaProperties (bbpms.workorder.sla.*) ----------
       Per-business-type overrides in wo_sla_policy are a v2 feature; work orders do not
       yet carry a business_type field. */
}
