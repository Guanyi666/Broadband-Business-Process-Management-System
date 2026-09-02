package com.bbpms.notify.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatSendResp {
    private Long messageId;
    private String status;
}