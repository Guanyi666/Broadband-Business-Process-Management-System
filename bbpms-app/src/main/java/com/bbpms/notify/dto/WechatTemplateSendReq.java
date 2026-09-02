package com.bbpms.notify.dto;
import lombok.Data;
import java.util.Map;
@Data
public class WechatTemplateSendReq {
    private String openId;
    private String templateId;
    private Map<String, Object> params;
    private String url;
}