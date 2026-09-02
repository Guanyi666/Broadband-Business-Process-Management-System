package com.bbpms.dispatch.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridge: when an order finishes auditing, kick off automatic dispatch.
 * Replaces the original RabbitMQ consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAuditedListener {

    private final DispatchService dispatchService;

    @Async
    @EventListener
    public void onOrderAudited(BbpmsEvents.OrderAuditedEvent event) {
        if (event == null || event.getOrderId() == null) return;
        log.info("OrderAuditedListener received orderId={}", event.getOrderId());
        try {
            dispatchService.autoDispatch(event.getOrderId());
        } catch (Exception e) {
            log.error("autoDispatch failed for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}