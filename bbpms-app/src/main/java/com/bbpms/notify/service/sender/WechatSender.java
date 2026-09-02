package com.bbpms.notify.service.sender;
import java.util.Map;
public interface WechatSender {
    SendResult send(String openId, String templateId, Map<String, Object> params, String url);
}