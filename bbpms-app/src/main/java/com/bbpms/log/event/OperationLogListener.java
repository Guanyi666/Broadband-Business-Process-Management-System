package com.bbpms.log.event;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogListener {
    private final OperationLogService service;
    @EventListener
    @Async
    public void onOperationLog(BbpmsEvents.OperationLogEvent event) {
        try { service.record(event); } catch (Exception e) { log.warn("OperationLogListener failed: {}", e.getMessage()); }
    }
}