package com.bbpms.dispatch.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridge: when an order finishes auditing, kick off automatic dispatch.
 * Replaces the original RabbitMQ consumer.
 *
 * <p>仅在审核事务 <b>提交后</b> 触发（AFTER_COMMIT）。否则异步派单会在审核事务
 * 提交前读取订单，拿到的是旧状态（CREATED），造成订单 dispatch_time 丢失、
 * 审计日志 from_status 错误、订单/工单状态不同步等一致性问题。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAuditedListener {

    private final DispatchService dispatchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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