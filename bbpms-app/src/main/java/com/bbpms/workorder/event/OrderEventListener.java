package com.bbpms.workorder.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.workorder.service.WorkOrderService;
import com.bbpms.workorder.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Replaces the original RabbitMQ consumers with Spring
 * {@link EventListener @EventListener} handlers.
 *
 * <p>Each event handler is marked
 * {@link Async @Async} so the publishing transaction (typically the
 * order-audit commit) is not blocked by downstream work.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final WorkOrderService workOrderService;
    /** Reserved for the future dispatch / scoring service. */
    // private final DispatchService dispatchService;

    /**
     * Triggered by {@link BbpmsEvents.OrderCreatedEvent}. Reserved for
     * any pre-audit side effects — actual work-order creation happens on
     * {@code OrderAuditedEvent}.
     */
    @EventListener
    @Async
    public void onOrderCreated(BbpmsEvents.OrderCreatedEvent e) {
        if (e == null || e.getOrderId() == null) {
            log.debug("OrderCreatedEvent without orderId, skipping");
        }
    }

    /**
     * NOTE (SA-P2-004): work-order creation on audit is NOT done here anymore.
     * Both this class and OrderAuditedListener used to listen to
     * {@code OrderAuditedEvent} — this one created a bare work order with a
     * null installer (never dispatchable) while the dispatch listener created a
     * complete one, so concurrent audit flows produced duplicate work orders and
     * records. Creation is now owned solely by {@code DispatchServiceImpl}
     * (autoDispatch creates the full work order inside an order-scoped lock).
     */

    /**
     * Triggered by {@link BbpmsEvents.OrderCancelledEvent}. Cancels any
     * outstanding work orders for the cancelled parent order.
     */
    @EventListener
    @Async
    public void onOrderCancelled(BbpmsEvents.OrderCancelledEvent e) {
        if (e == null || e.getOrderId() == null) return;
        try {
            WorkOrderVO wo = workOrderService.findByOrderId(e.getOrderId());
            if (wo != null && wo.getId() != null) {
                workOrderService.cancel(wo.getId(), e.getOperatorId(), e.getReason());
                log.info("Order {} cancelled -> work order {} cancelled",
                        e.getOrderId(), wo.getWorkNo());
            }
        } catch (Exception ex) {
            log.error("Failed to cancel work order for cancelled orderId={}: {}",
                    e.getOrderId(), ex.getMessage(), ex);
        }
    }

    /**
     * On a {@link BbpmsEvents.InstallCompletedEvent} we mark the parent
     * work order as completed too — installed-service side effect.
     */
    @EventListener
    @Async
    public void onInstallCompleted(BbpmsEvents.InstallCompletedEvent e) {
        if (e == null || e.getWorkOrderId() == null) return;
        try {
            workOrderService.complete(e.getWorkOrderId(), e.getInstallerId(), null);
            log.info("Work order {} marked completed by install event", e.getWorkOrderId());
        } catch (Exception ex) {
            log.error("Failed to mark complete workOrderId={}: {}",
                    e.getWorkOrderId(), ex.getMessage(), ex);
        }
    }
}