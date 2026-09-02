package com.bbpms.notify.service.sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "bbpms.wechat.app-id", havingValue = "", matchIfMissing = true)
public class MockWechatSender implements WechatSender {
    @Override
    public SendResult send(String openId, String templateId, Map<String, Object> params, String url) {
        log.info("[MOCK WECHAT] openId={} template={} params={}", openId, templateId, params);
        return new SendResult(true, "mock-" + UUID.randomUUID(), null);
    }
}