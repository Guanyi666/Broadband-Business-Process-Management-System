package com.bbpms.notify.service.sender;
import java.util.Map;
public interface SmsSender {
    SendResult send(String phone, String templateCode, Map<String, Object> params);
}