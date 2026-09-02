package com.bbpms.notify.dto;
import lombok.Data;
import java.util.Map;
@Data
public class SmsSendReq {
    private String phone;
    private String templateCode;
    private Map<String, Object> params;
}