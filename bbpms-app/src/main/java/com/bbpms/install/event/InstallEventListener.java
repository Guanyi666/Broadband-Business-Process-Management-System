package com.bbpms.install.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.install.service.InstallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * When a work order is accepted, lazily seed a PENDING install record so the
 * installer flow has a stable row to update. Listens AFTER_COMMIT so we
 * don't pre-create rows for work orders whose dispatch eventually rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallEventListener {

    private final InstallService installService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccepted(BbpmsEvents.WorkOrderAcceptedEvent event) {
        if (event == null || event.getWorkOrderId() == null) return;
        log.info("InstallEventListener.onAccepted workOrderId={} installerId={}",
                event.getWorkOrderId(), event.getInstallerId());
        try {
            installService.initRecord(event.getWorkOrderId(), event.getInstallerId());
        } catch (Exception e) {
            log.error("initRecord failed for workOrderId={}: {}",
                    event.getWorkOrderId(), e.getMessage(), e);
        }
    }
}