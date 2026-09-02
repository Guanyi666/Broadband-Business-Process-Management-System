package com.bbpms.install.bss;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.install.config.InstallProperties;
import com.bbpms.install.dto.BssActivateReq;
import com.bbpms.install.dto.BssActivateResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in BSS client. Simulates latency and a configurable failure rate so
 * the saga replacement / compensation path can be verified end-to-end without
 * a real provisioning API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bbpms.bss.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockBssClient implements BssClient {

    private final InstallProperties props;

    @Override
    public BssActivateResp activate(BssActivateReq req) {
        InstallProperties.Bss bss = props.getBss();
        long min = bss == null || bss.getMinDelayMs() == null ? 200L : bss.getMinDelayMs();
        long max = bss == null || bss.getMaxDelayMs() == null ? 2000L : Math.max(min, bss.getMaxDelayMs());
        double failureRate = bss == null || bss.getFailureRate() == null ? 0.05 : bss.getFailureRate();

        try {
            long sleep = ThreadLocalRandom.current().nextLong(min, max + 1);
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.INTERNAL_ERROR, "BSS_INTERRUPTED");
        }

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("MockBssClient simulated failure for orderId={}", req == null ? null : req.getOrderId());
            // Alternate the failure shape so both code paths are exercised in dev.
            if (ThreadLocalRandom.current().nextBoolean()) {
                throw new BizException(ResultCode.SMS_SEND_FAILED, "BSS_PROVISIONING_REJECTED");
            }
            throw new BizException(ResultCode.INTERNAL_ERROR, "BSS_ACTIVATION_FAILED");
        }

        BssActivateResp resp = new BssActivateResp();
        resp.setSuccess(true);
        resp.setOrderId(req == null ? null : req.getOrderId());
        resp.setMessage("activated");
        resp.setActivatedAt(LocalDateTime.now());
        return resp;
    }
}