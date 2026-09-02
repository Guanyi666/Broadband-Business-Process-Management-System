package com.bbpms.notify.dto;
import lombok.Data;
@Data
public class MessageTemplateCreateReq {
    private Long id;
    private String code;
    private String channel;
    private String subject;
    private String content;
    private String aliyunTemplateId;
    private String wechatTemplateId;
    private Integer enabled;
}