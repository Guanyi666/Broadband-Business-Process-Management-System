package com.bbpms.notify.service.sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "bbpms.aliyun.sms.access-key", havingValue = "", matchIfMissing = true)
public class MockSmsSender implements SmsSender {
    @Override
    public SendResult send(String phone, String templateCode, Map<String, Object> params) {
        log.info("[MOCK SMS] to={} template={} params={}", phone, templateCode, params);
        return new SendResult(true, "mock-" + UUID.randomUUID(), null);
    }
}