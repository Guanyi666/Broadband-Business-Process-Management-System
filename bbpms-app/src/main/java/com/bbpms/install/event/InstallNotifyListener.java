package com.bbpms.install.event;

import com.bbpms.common.event.BbpmsEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges InstallCompletedEvent into a customer-facing NotifyEvent. The
 * notification module is the real consumer — this just builds the payload
 * asynchronously off the install transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallNotifyListener {

    private final ApplicationEventPublisher publisher;

    @Async
    @EventListener
    public void onInstallCompleted(BbpmsEvents.InstallCompletedEvent event) {
        if (event == null || event.getWorkOrderId() == null) return;
        log.info("InstallNotifyListener fan-out for workOrderId={}", event.getWorkOrderId());
        Map<String, Object> params = new HashMap<>();
        params.put("workOrderId", event.getWorkOrderId());
        params.put("orderId", event.getOrderId());
        params.put("onuMac", event.getOnuMac());
        publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                "SMS", null, null, event.getInstallerId(),
                "INSTALL_COMPLETED_SMS", params));
        publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                "WECHAT", null, null, event.getInstallerId(),
                "INSTALL_COMPLETED_WX", params));
    }
}