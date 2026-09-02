package com.bbpms.notify.event;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyEventListener {
    private final NotifyService notifyService;
    @EventListener
    @Async
    public void onNotifyEvent(BbpmsEvents.NotifyEvent event) {
        try {
            notifyService.handleNotifyEvent(event);
        } catch (Exception e) {
            log.warn("NotifyEventListener failed: {}", e.getMessage());
        }
    }
}